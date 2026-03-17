package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateStatus;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Response DTO for SMS templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateResponse {

    private Long id;
    private String templateCode;
    private String name;
    private String content;
    private SmsTemplateType templateType;
    private SmsProviderType provider;
    private String providerTemplateId;
    private SmsTemplateStatus status;
    private Boolean active;
    private List<String> variables;
    private String language;
    private String description;
    private String rejectionReason;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert entity to response DTO.
     */
    public static SmsTemplateResponse fromEntity(SmsTemplate template) {
        List<String> variableList = Collections.emptyList();
        if (template.getVariables() != null && !template.getVariables().isBlank()) {
            variableList = Arrays.asList(template.getVariables().split(","));
        }

        return SmsTemplateResponse.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .name(template.getName())
                .content(template.getContent())
                .templateType(template.getTemplateType())
                .provider(template.getProvider())
                .providerTemplateId(template.getProviderTemplateId())
                .status(template.getStatus())
                .active(template.getActive())
                .variables(variableList)
                .language(template.getLanguage())
                .description(template.getDescription())
                .rejectionReason(template.getRejectionReason())
                .lastSyncedAt(template.getLastSyncedAt())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
