package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for template sync operations with SMS providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateSyncResponse {

    private boolean success;
    private String providerTemplateId;
    private SmsTemplateStatus status;
    private String message;
    private String errorCode;
    private String provider;

    public static SmsTemplateSyncResponse success(String providerTemplateId, SmsTemplateStatus status, String provider) {
        return SmsTemplateSyncResponse.builder()
                .success(true)
                .providerTemplateId(providerTemplateId)
                .status(status)
                .provider(provider)
                .build();
    }

    public static SmsTemplateSyncResponse failure(String message, String errorCode, String provider) {
        return SmsTemplateSyncResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(SmsTemplateStatus.DRAFT)
                .provider(provider)
                .build();
    }
}
