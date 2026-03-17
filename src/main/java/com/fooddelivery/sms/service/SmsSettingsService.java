package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.DevSmsProperties;
import com.fooddelivery.sms.config.EskizSmsProperties;
import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.entity.SmsProviderSetting;
import com.fooddelivery.sms.repository.SmsProviderSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing SMS provider settings with database persistence.
 * Loads settings from database on startup and persists changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSettingsService {

    private final SmsProviderSettingRepository settingRepository;
    private final SmsProperties smsProperties;
    private final DevSmsProperties devSmsProperties;
    private final EskizSmsProperties eskizSmsProperties;

    /**
     * Load settings from database on application startup.
     */
    @PostConstruct
    public void loadSettingsFromDatabase() {
        log.info("Loading SMS provider settings from database...");

        try {
            // Load main SMS settings
            loadMainSettings();

            // Load DevSMS settings
            loadDevSmsSettings();

            // Load Eskiz settings
            loadEskizSettings();

            log.info("SMS provider settings loaded successfully. Provider: {}, Enabled: {}",
                    smsProperties.getProvider(), smsProperties.isEnabled());
        } catch (Exception e) {
            log.warn("Could not load SMS settings from database, using defaults: {}", e.getMessage());
        }
    }

    private void loadMainSettings() {
        getSettingValue("sms.enabled").ifPresent(value ->
                smsProperties.setEnabled(Boolean.parseBoolean(value)));

        getSettingValue("sms.provider").ifPresent(value -> {
            try {
                smsProperties.setProvider(SmsProviderType.valueOf(value));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid SMS provider type in database: {}", value);
            }
        });

        getSettingValue("sms.fallback_enabled").ifPresent(value ->
                smsProperties.setFallbackEnabled(Boolean.parseBoolean(value)));

        getSettingValue("sms.fallback_provider").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                try {
                    smsProperties.setFallbackProvider(SmsProviderType.valueOf(value));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid fallback provider type in database: {}", value);
                }
            }
        });
    }

    private void loadDevSmsSettings() {
        getSettingValue("devsms.enabled").ifPresent(value ->
                devSmsProperties.setEnabled(Boolean.parseBoolean(value)));

        getSettingValue("devsms.base_url").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                devSmsProperties.setBaseUrl(value);
            }
        });

        getSettingValue("devsms.token").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                devSmsProperties.setToken(value);
            }
        });

        getSettingValue("devsms.from").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                devSmsProperties.setFrom(value);
            }
        });

        getSettingValue("devsms.callback_url").ifPresent(devSmsProperties::setCallbackUrl);
    }

    private void loadEskizSettings() {
        getSettingValue("eskiz.enabled").ifPresent(value ->
                eskizSmsProperties.setEnabled(Boolean.parseBoolean(value)));

        getSettingValue("eskiz.base_url").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                eskizSmsProperties.setBaseUrl(value);
            }
        });

        getSettingValue("eskiz.email").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                eskizSmsProperties.setEmail(value);
            }
        });

        getSettingValue("eskiz.password").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                eskizSmsProperties.setPassword(value);
            }
        });

        getSettingValue("eskiz.from").ifPresent(value -> {
            if (value != null && !value.isBlank()) {
                eskizSmsProperties.setFrom(value);
            }
        });

        getSettingValue("eskiz.callback_url").ifPresent(eskizSmsProperties::setCallbackUrl);
    }

    /**
     * Get a setting value from the database.
     */
    public Optional<String> getSettingValue(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SmsProviderSetting::getSettingValue);
    }

    /**
     * Save a setting to the database.
     */
    @Transactional
    public void saveSetting(String key, String value) {
        SmsProviderSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> SmsProviderSetting.builder()
                        .settingKey(key)
                        .build());

        setting.setSettingValue(value);
        settingRepository.save(setting);
        log.debug("Saved SMS setting: {} = {}", key, maskSensitive(key, value));
    }

    /**
     * Update main SMS settings and persist to database.
     */
    @Transactional
    public void updateMainSettings(Boolean enabled, SmsProviderType provider,
                                   Boolean fallbackEnabled, SmsProviderType fallbackProvider) {
        if (enabled != null) {
            smsProperties.setEnabled(enabled);
            saveSetting("sms.enabled", String.valueOf(enabled));
        }

        if (provider != null) {
            smsProperties.setProvider(provider);
            saveSetting("sms.provider", provider.name());
        }

        if (fallbackEnabled != null) {
            smsProperties.setFallbackEnabled(fallbackEnabled);
            saveSetting("sms.fallback_enabled", String.valueOf(fallbackEnabled));
        }

        if (fallbackProvider != null) {
            smsProperties.setFallbackProvider(fallbackProvider);
            saveSetting("sms.fallback_provider", fallbackProvider.name());
        }

        log.info("Main SMS settings updated and persisted");
    }

    /**
     * Update DevSMS settings and persist to database.
     */
    @Transactional
    public void updateDevSmsSettings(Boolean enabled, String token, String from,
                                     String baseUrl, String callbackUrl) {
        if (enabled != null) {
            devSmsProperties.setEnabled(enabled);
            saveSetting("devsms.enabled", String.valueOf(enabled));
        }

        if (token != null) {
            devSmsProperties.setToken(token);
            saveSetting("devsms.token", token);
        }

        if (from != null) {
            devSmsProperties.setFrom(from);
            saveSetting("devsms.from", from);
        }

        if (baseUrl != null) {
            devSmsProperties.setBaseUrl(baseUrl);
            saveSetting("devsms.base_url", baseUrl);
        }

        if (callbackUrl != null) {
            devSmsProperties.setCallbackUrl(callbackUrl);
            saveSetting("devsms.callback_url", callbackUrl);
        }

        log.info("DevSMS settings updated and persisted");
    }

    /**
     * Update Eskiz settings and persist to database.
     */
    @Transactional
    public void updateEskizSettings(Boolean enabled, String email, String password,
                                    String from, String baseUrl, String callbackUrl) {
        if (enabled != null) {
            eskizSmsProperties.setEnabled(enabled);
            saveSetting("eskiz.enabled", String.valueOf(enabled));
        }

        if (email != null) {
            eskizSmsProperties.setEmail(email);
            saveSetting("eskiz.email", email);
        }

        if (password != null) {
            eskizSmsProperties.setPassword(password);
            saveSetting("eskiz.password", password);
        }

        if (from != null) {
            eskizSmsProperties.setFrom(from);
            saveSetting("eskiz.from", from);
        }

        if (baseUrl != null) {
            eskizSmsProperties.setBaseUrl(baseUrl);
            saveSetting("eskiz.base_url", baseUrl);
        }

        if (callbackUrl != null) {
            eskizSmsProperties.setCallbackUrl(callbackUrl);
            saveSetting("eskiz.callback_url", callbackUrl);
        }

        log.info("Eskiz settings updated and persisted");
    }

    /**
     * Mask sensitive values for logging.
     */
    private String maskSensitive(String key, String value) {
        if (value == null) {
            return "null";
        }
        if (key.contains("token") || key.contains("password")) {
            return "****" + (value.length() > 4 ? value.substring(value.length() - 4) : "");
        }
        return value;
    }
}
