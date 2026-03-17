package com.fooddelivery.sms.entity;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for SMS templates that can be synced with SMS providers.
 * Both Eskiz and DevSMS support template-based messaging.
 */
@Entity
@Table(name = "sms_templates",
        indexes = {
                @Index(name = "idx_sms_template_code", columnList = "template_code"),
                @Index(name = "idx_sms_template_type", columnList = "template_type"),
                @Index(name = "idx_sms_template_provider", columnList = "provider")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sms_template_code_provider",
                        columnNames = {"template_code", "provider"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique template code/identifier used by the SMS provider.
     */
    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    /**
     * Human-readable template name.
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Template content with placeholders.
     * Placeholders format: {variable_name}
     * Example: "Your OTP code is {code}. Valid for {minutes} minutes."
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Type of SMS this template is used for.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    private SmsTemplateType templateType;

    /**
     * SMS provider this template belongs to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SmsProviderType provider;

    /**
     * Provider's internal template ID (returned after registration).
     */
    @Column(name = "provider_template_id", length = 100)
    private String providerTemplateId;

    /**
     * Template status on the provider side.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SmsTemplateStatus status = SmsTemplateStatus.DRAFT;

    /**
     * Whether this template is active in our system.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Template variables/placeholders (comma-separated).
     * Example: "code,minutes,app_name"
     */
    @Column(name = "variables", length = 500)
    private String variables;

    /**
     * Language code for the template.
     */
    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "uz";

    /**
     * Optional description for admin reference.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Rejection reason if template was rejected by provider.
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Last sync attempt with provider.
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Types of SMS templates.
     */
    public enum SmsTemplateType {
        OTP,
        ORDER_CONFIRMATION,
        ORDER_STATUS,
        DELIVERY_UPDATE,
        PAYMENT_CONFIRMATION,
        WELCOME,
        PASSWORD_RESET,
        PROMOTIONAL,
        GENERAL
    }

    /**
     * Template status on provider side.
     */
    public enum SmsTemplateStatus {
        DRAFT,           // Not yet submitted to provider
        PENDING,         // Submitted, awaiting approval
        APPROVED,        // Approved and ready to use
        REJECTED,        // Rejected by provider
        SUSPENDED        // Suspended by provider
    }
}
