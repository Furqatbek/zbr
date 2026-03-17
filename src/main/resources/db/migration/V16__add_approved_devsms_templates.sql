-- Re-add DevSMS templates with APPROVED status
-- DevSMS has no template management API, so templates are auto-approved locally
-- Previous V13 migration created them as DRAFT, and V15 deleted all DRAFT templates

INSERT INTO sms_templates (template_code, name, content, template_type, provider, status, provider_template_id, variables, description, last_synced_at)
VALUES
    ('OTP_SIGNUP', 'OTP Signup', 'Food Delivery: Sizning tasdiqlash kodingiz: {code}. Kod 5 daqiqa davomida amal qiladi.', 'OTP', 'DEVSMS', 'APPROVED', 'DEVSMS-OTP_SIGNUP', 'code', 'OTP code for user signup', CURRENT_TIMESTAMP),
    ('OTP_LOGIN', 'OTP Login', 'Food Delivery: Kirish uchun tasdiqlash kodi: {code}', 'OTP', 'DEVSMS', 'APPROVED', 'DEVSMS-OTP_LOGIN', 'code', 'OTP code for user login', CURRENT_TIMESTAMP),
    ('ORDER_CONFIRMED', 'Order Confirmed', 'Buyurtma #{order_id} qabul qilindi. {restaurant} dan yetkazib berish taxminan {eta} daqiqada.', 'ORDER_CONFIRMATION', 'DEVSMS', 'APPROVED', 'DEVSMS-ORDER_CONFIRMED', 'order_id,restaurant,eta', 'Order confirmation message', CURRENT_TIMESTAMP),
    ('DELIVERY_UPDATE', 'Delivery Update', 'Buyurtma #{order_id}: {status}. Kuryer: {courier_name}, Tel: {courier_phone}', 'DELIVERY_UPDATE', 'DEVSMS', 'APPROVED', 'DEVSMS-DELIVERY_UPDATE', 'order_id,status,courier_name,courier_phone', 'Delivery status update', CURRENT_TIMESTAMP),
    ('PASSWORD_RESET', 'Password Reset', 'Food Delivery: Parolni tiklash kodi: {code}. Kod 1 soat davomida amal qiladi.', 'PASSWORD_RESET', 'DEVSMS', 'APPROVED', 'DEVSMS-PASSWORD_RESET', 'code', 'Password reset code', CURRENT_TIMESTAMP),
    ('WELCOME', 'Welcome Message', 'Food Delivery ga xush kelibsiz, {name}! Eng yaxshi taomlarni buyurtma qiling.', 'WELCOME', 'DEVSMS', 'APPROVED', 'DEVSMS-WELCOME', 'name', 'Welcome message for new users', CURRENT_TIMESTAMP);
