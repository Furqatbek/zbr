package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating SMS templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateRequest {

    /**
     * Unique template code/identifier.
     */
    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    private String templateCode;

    /**
     * Human-readable template name.
     */
    @NotBlank(message = "Template name is required")
    @Size(max = 200, message = "Template name must not exceed 200 characters")
    private String name;

    /**
     * Template content with placeholders.
     * Placeholders format: {variable_name}
     */
    @NotBlank(message = "Template content is required")
    private String content;

    /**
     * Type of SMS this template is used for.
     */
    @NotNull(message = "Template type is required")
    private SmsTemplateType templateType;

    /**
     * SMS provider this template belongs to.
     */
    @NotNull(message = "Provider is required")
    private SmsProviderType provider;

    /**
     * Template variables/placeholders.
     */
    private List<String> variables;

    /**
     * Language code for the template.
     */
    @Builder.Default
    private String language = "uz";

    /**
     * Optional description for admin reference.
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Whether to immediately sync with provider after creation.
     */
    @Builder.Default
    private boolean syncImmediately = false;
}
