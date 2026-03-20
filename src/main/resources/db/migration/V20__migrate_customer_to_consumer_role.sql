-- Migration to update notification role from CUSTOMER to CONSUMER
-- This aligns the notification system with the auth system role naming

-- Update existing notifications with role='CUSTOMER' to 'CONSUMER'
UPDATE notifications
SET role = 'CONSUMER'
WHERE role = 'CUSTOMER';
