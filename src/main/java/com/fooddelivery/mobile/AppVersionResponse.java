package com.fooddelivery.mobile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the app needs to decide whether to prompt for an update.
 *
 * <p>The comparison itself stays on the device: it knows its own installed
 * version, and a server that decided for it would have to be told that version
 * on every check. We answer with facts about the store instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Latest and minimum supported app version for a platform")
public class AppVersionResponse {

    @Schema(description = "Newest version live in this platform's store", example = "1.0.1")
    private String latestVersion;

    @Schema(description = "Oldest version still allowed to run", example = "1.0.0")
    private String minimumVersion;

    @Schema(description = "This platform's store link. Omitted when not configured, "
            + "in which case the app uses its own built-in link")
    private String storeUrl;
}
