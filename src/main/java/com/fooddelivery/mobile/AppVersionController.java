package com.fooddelivery.mobile;

import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * What version of the app the store has, for the app to compare against itself.
 *
 * <p><strong>Unauthenticated, deliberately.</strong> The check runs on launch
 * and on the login screen, before anyone has a token. An update prompt that
 * only appears after signing in cannot help a customer whose version is too old
 * to sign in.
 */
@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mobile app", description = "Version checks for the customer app")
public class AppVersionController {

    private final MobileVersionProperties properties;

    /**
     * @param platform ios or android, required — see {@link MobilePlatform} for
     *        why there is no shared answer
     */
    @GetMapping("/version")
    @Operation(summary = "Latest and minimum app version",
            description = "Public. Answers for the platform asked about, because the two stores "
                    + "are never in lockstep.")
    public ResponseEntity<ApiResponse<AppVersionResponse>> version(
            @Parameter(description = "ios or android", required = true)
            @RequestParam(required = false) String platform,
            @Parameter(description = "customer, courier or vendor — defaults to customer")
            @RequestParam(required = false) String app) {

        MobilePlatform resolved = MobilePlatform.from(platform);
        MobileVersionProperties.Release release =
                properties.forApp(MobileApp.from(app), resolved);

        AppVersionResponse body = AppVersionResponse.builder()
                .latestVersion(release.getLatest())
                .minimumVersion(release.getMinimum())
                // Null is omitted from the response, and the app then uses its
                // own link. A store URL for the wrong platform is discarded by
                // the client anyway — it checks the host.
                .storeUrl(release.getStoreUrl() == null || release.getStoreUrl().isBlank()
                        ? null : release.getStoreUrl())
                .build();

        // Cheap, identical for every caller, and read on every launch and
        // resume. A short cache costs a few minutes of propagation on a release
        // and saves the round trip the rest of the time.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(properties.getCacheSeconds(), TimeUnit.SECONDS)
                        .cachePublic())
                .body(ApiResponse.success(body));
    }
}
