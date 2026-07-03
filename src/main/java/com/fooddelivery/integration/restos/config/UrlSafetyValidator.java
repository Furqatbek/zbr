package com.fooddelivery.integration.restos.config;

import com.fooddelivery.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * SSRF guard for user-supplied outbound URLs (Restos integration).
 *
 * Rejects anything that could reach internal infrastructure: non-http(s)
 * schemes, missing host, cloud metadata endpoints, and any host that resolves
 * to a loopback / link-local / site-local / private / any-local / multicast
 * address. Optionally enforces a hostname allowlist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UrlSafetyValidator {

    private final RestosProperties properties;

    /**
     * Validate that {@code rawUrl} is safe to fetch server-side.
     * @throws BusinessException if the URL is malformed or targets a non-public destination.
     */
    public void validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException("URL is required");
        }

        final URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (Exception e) {
            throw new BusinessException("Invalid URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BusinessException("Only http/https URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException("URL must contain a valid host");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);

        // Optional allowlist
        var allowed = properties.getAllowedHosts();
        if (allowed != null && !allowed.isEmpty()) {
            boolean ok = allowed.stream().anyMatch(h -> h != null && h.equalsIgnoreCase(host));
            if (!ok) {
                throw new BusinessException("Host '" + host + "' is not in the allowed Restos hosts list");
            }
        }

        // Block obvious metadata / local hostnames before DNS resolution
        if (normalizedHost.equals("localhost")
                || normalizedHost.equals("metadata.google.internal")
                || normalizedHost.endsWith(".local")
                || normalizedHost.endsWith(".internal")) {
            throw new BusinessException("URL targets a forbidden internal host");
        }

        // Resolve and inspect every returned address
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new BusinessException("Could not resolve host: " + host);
        }
        for (InetAddress addr : addresses) {
            if (isForbidden(addr)) {
                log.warn("SSRF blocked: host {} resolved to non-public address {}", host, addr.getHostAddress());
                throw new BusinessException("URL resolves to a non-public (internal) address and is not allowed");
            }
        }
    }

    private boolean isForbidden(InetAddress addr) {
        if (addr.isAnyLocalAddress()      // 0.0.0.0 / ::
                || addr.isLoopbackAddress()   // 127.0.0.0/8, ::1
                || addr.isLinkLocalAddress()  // 169.254.0.0/16 (incl. cloud metadata 169.254.169.254), fe80::
                || addr.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16
                || addr.isMulticastAddress()) {
            return true;
        }
        // Unique-local IPv6 (fc00::/7) and other private ranges isSiteLocalAddress misses
        byte[] b = addr.getAddress();
        if (b.length == 16) {
            int first = b[0] & 0xff;
            if ((first & 0xfe) == 0xfc) { // fc00::/7
                return true;
            }
        }
        return false;
    }
}
