-- SMS Templates table for managing provider templates
CREATE TABLE sms_templates (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_template_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    variables VARCHAR(500),
    language VARCHAR(10) DEFAULT 'uz',
    description VARCHAR(500),
    rejection_reason VARCHAR(500),
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_sms_template_code_provider UNIQUE (template_code, provider)
);

-- Indexes for common queries
CREATE INDEX idx_sms_template_code ON sms_templates(template_code);
CREATE INDEX idx_sms_template_type ON sms_templates(template_type);
CREATE INDEX idx_sms_template_provider ON sms_templates(provider);
CREATE INDEX idx_sms_template_status ON sms_templates(status);

-- Add comments
COMMENT ON TABLE sms_templates IS 'SMS templates for Eskiz and DevSMS providers';
COMMENT ON COLUMN sms_templates.template_code IS 'Unique template code/identifier';
COMMENT ON COLUMN sms_templates.content IS 'Template content with placeholders like {code}, {name}';
COMMENT ON COLUMN sms_templates.provider_template_id IS 'Template ID returned by the SMS provider after registration';
COMMENT ON COLUMN sms_templates.status IS 'DRAFT, PENDING, APPROVED, REJECTED, SUSPENDED';
COMMENT ON COLUMN sms_templates.variables IS 'Comma-separated list of template variables';

-- Insert default OTP templates for both providers
INSERT INTO sms_templates (template_code, name, content, template_type, provider, status, variables, description)
VALUES
    ('OTP_SIGNUP', 'OTP Signup', 'Food Delivery: Sizning tasdiqlash kodingiz: {code}. Kod 5 daqiqa davomida amal qiladi.', 'OTP', 'ESKIZ', 'DRAFT', 'code', 'OTP code for user signup'),
    ('OTP_SIGNUP', 'OTP Signup', 'Food Delivery: Sizning tasdiqlash kodingiz: {code}. Kod 5 daqiqa davomida amal qiladi.', 'OTP', 'DEVSMS', 'DRAFT', 'code', 'OTP code for user signup'),
    ('OTP_LOGIN', 'OTP Login', 'Food Delivery: Kirish uchun tasdiqlash kodi: {code}', 'OTP', 'ESKIZ', 'DRAFT', 'code', 'OTP code for user login'),
    ('OTP_LOGIN', 'OTP Login', 'Food Delivery: Kirish uchun tasdiqlash kodi: {code}', 'OTP', 'DEVSMS', 'DRAFT', 'code', 'OTP code for user login'),
    ('ORDER_CONFIRMED', 'Order Confirmed', 'Buyurtma #{order_id} qabul qilindi. {restaurant} dan yetkazib berish taxminan {eta} daqiqada.', 'ORDER_CONFIRMATION', 'ESKIZ', 'DRAFT', 'order_id,restaurant,eta', 'Order confirmation message'),
    ('ORDER_CONFIRMED', 'Order Confirmed', 'Buyurtma #{order_id} qabul qilindi. {restaurant} dan yetkazib berish taxminan {eta} daqiqada.', 'ORDER_CONFIRMATION', 'DEVSMS', 'DRAFT', 'order_id,restaurant,eta', 'Order confirmation message'),
    ('DELIVERY_UPDATE', 'Delivery Update', 'Buyurtma #{order_id}: {status}. Kuryer: {courier_name}, Tel: {courier_phone}', 'DELIVERY_UPDATE', 'ESKIZ', 'DRAFT', 'order_id,status,courier_name,courier_phone', 'Delivery status update'),
    ('DELIVERY_UPDATE', 'Delivery Update', 'Buyurtma #{order_id}: {status}. Kuryer: {courier_name}, Tel: {courier_phone}', 'DELIVERY_UPDATE', 'DEVSMS', 'DRAFT', 'order_id,status,courier_name,courier_phone', 'Delivery status update'),
    ('PASSWORD_RESET', 'Password Reset', 'Food Delivery: Parolni tiklash kodi: {code}. Kod 1 soat davomida amal qiladi.', 'PASSWORD_RESET', 'ESKIZ', 'DRAFT', 'code', 'Password reset code'),
    ('PASSWORD_RESET', 'Password Reset', 'Food Delivery: Parolni tiklash kodi: {code}. Kod 1 soat davomida amal qiladi.', 'PASSWORD_RESET', 'DEVSMS', 'DRAFT', 'code', 'Password reset code'),
    ('WELCOME', 'Welcome Message', 'Food Delivery ga xush kelibsiz, {name}! Eng yaxshi taomlarni buyurtma qiling.', 'WELCOME', 'ESKIZ', 'DRAFT', 'name', 'Welcome message for new users'),
    ('WELCOME', 'Welcome Message', 'Food Delivery ga xush kelibsiz, {name}! Eng yaxshi taomlarni buyurtma qiling.', 'WELCOME', 'DEVSMS', 'DRAFT', 'name', 'Welcome message for new users');
