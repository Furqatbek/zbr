-- Remove all pre-created SMS templates
-- Templates should be created manually via API with real provider template IDs
-- after registering them on the SMS provider's platform (Eskiz/DevSMS)

DELETE FROM sms_templates;
