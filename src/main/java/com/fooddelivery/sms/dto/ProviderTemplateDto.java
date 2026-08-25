package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One template as it exists ON THE PROVIDER.
 *
 * <p>Distinct from {@link SmsTemplateSyncResponse}, which reports the outcome of
 * a sync and carries only an id and a status. Importing needs the actual text —
 * without it an import produces rows with no content, which is worse than no
 * import at all because they look real and send nothing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderTemplateDto {

    /** The provider's own id for this template. */
    private String providerTemplateId;

    /** Human-readable name, where the provider has one. */
    private String name;

    /** The message text as registered with the provider. */
    private String content;

    /** Approval state, mapped to our vocabulary. */
    private SmsTemplateStatus status;
}
