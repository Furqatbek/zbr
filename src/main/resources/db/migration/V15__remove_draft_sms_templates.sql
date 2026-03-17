-- Remove draft SMS templates that were added in V13
-- These templates were never synced/approved by the SMS provider

DELETE FROM sms_templates WHERE status = 'DRAFT';
