package com.fooddelivery.integration.partner.client;

import com.fooddelivery.integration.partner.dto.OutboundOrder;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.restos.config.UrlSafetyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * POSTs an order to a partner's POS.
 *
 * <p>Its whole job is turning an HTTP outcome into one of two answers: worth
 * retrying, or not. Everything upstream depends on that distinction — retrying
 * a rejection forever hides a real problem behind a queue that looks busy,
 * while giving up on a timeout loses an order nobody will cook.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerOrderPushClient {

    /** Reused from the menu integration: same timeouts, same SSRF posture. */
    private final @Qualifier("restosRestTemplate") RestTemplate restTemplate;
    private final UrlSafetyValidator urlSafetyValidator;

    public sealed interface Result {

        /** Accepted. {@code partnerOrderId} is their name for it, when given. */
        record Accepted(String partnerOrderId, boolean duplicate) implements Result {
        }

        /** Worth another attempt: a timeout, a 5xx, a refused connection. */
        record Retryable(String reason) implements Result {
        }

        /** They will refuse it again. A person has to look at this one. */
        record Rejected(String reason) implements Result {
        }
    }

    public Result push(Partner partner, OutboundOrder order) {
        String url = endpoint(partner);
        if (url == null) {
            return new Result.Rejected("Partner " + partner.getCode()
                    + " has no outbound base URL configured");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            applyCredential(partner, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(order, headers),
                    new org.springframework.core.ParameterizedTypeReference<>() {});

            // Their contract: a replay returns 200 with duplicate:true, where a
            // fresh order returns 201. Both mean the ticket is in the kitchen,
            // and treating the replay as a failure would have us retry forever
            // against an order that is already cooking.
            Map<String, Object> body = response.getBody();
            boolean duplicate = body != null && Boolean.TRUE.equals(body.get("duplicate"));
            return new Result.Accepted(partnerOrderId(body), duplicate);

        } catch (HttpClientErrorException e) {
            // 4xx is the partner telling us the request is wrong. Sending it
            // again unchanged produces the same answer — except 408 and 429,
            // which are explicitly "later".
            HttpStatusCode status = e.getStatusCode();
            String detail = status + " " + truncate(e.getResponseBodyAsString());
            if (status.value() == 408 || status.value() == 429) {
                return new Result.Retryable(detail);
            }
            log.warn("Partner {} refused order {}: {}", partner.getCode(), order.getExternalOrderId(), detail);
            return new Result.Rejected(detail);

        } catch (HttpServerErrorException e) {
            return new Result.Retryable(e.getStatusCode() + " " + truncate(e.getResponseBodyAsString()));

        } catch (ResourceAccessException e) {
            // Timeout, DNS, connection refused. Their system being down is the
            // ordinary case this whole retry machinery exists for.
            return new Result.Retryable(e.getMessage());

        } catch (Exception e) {
            // An unexpected shape of failure — a parse error, say. Retryable
            // rather than rejected: the cost of one wasted retry is far below
            // the cost of dropping an order because of a bug on our side.
            log.error("Unexpected failure pushing order {} to {}",
                    order.getExternalOrderId(), partner.getCode(), e);
            return new Result.Retryable(e.toString());
        }
    }

    private String endpoint(Partner partner) {
        String base = partner.getOutboundBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        // Admin-configured rather than user-supplied, so this is not the acute
        // SSRF case the menu import guards against — but an operator pasting an
        // internal address should still be refused rather than handed our
        // metadata service.
        urlSafetyValidator.validate(normalized);
        return normalized + "/api/v1/partner/orders";
    }

    private void applyCredential(Partner partner, HttpHeaders headers) {
        String key = partner.getOutboundApiKey();
        if (key == null || key.isBlank()) {
            return;
        }
        String header = partner.getOutboundAuthHeader();
        if (header == null || header.isBlank() || HttpHeaders.AUTHORIZATION.equalsIgnoreCase(header)) {
            headers.setBearerAuth(key);
        } else {
            headers.set(header, key);
        }
    }

    @SuppressWarnings("unchecked")
    private String partnerOrderId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object direct = body.get("orderId");
        if (direct != null) {
            return String.valueOf(direct);
        }
        // Their envelope may nest it, as ours does.
        if (body.get("data") instanceof Map<?, ?> data) {
            Object nested = ((Map<String, Object>) data).get("orderId");
            return nested != null ? String.valueOf(nested) : null;
        }
        return null;
    }

    /** Error text goes in a VARCHAR(1000) and into logs; their body may not. */
    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 400 ? body : body.substring(0, 400) + "…";
    }
}
