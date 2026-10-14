-- Flyway Migration V4: Seed Initial Users
-- admin / Admin@123
-- user / User@123
INSERT INTO app_users (username, password, email, role)
VALUES 
    ('admin', '$2a$10$hbxecwitQQ.dDT4JOFzQAulNySFwEpaFLw38jda6Td.Y/cOiRzDFu', 'admin@medical-inventory.com', 'ROLE_ADMIN'),
    ('user', '$2a$10$hbxecwitQQ.dDT4JOFzQAulNySFwEpaFLw38jda6Td.Y/cOiRzDFu', 'user@medical-inventory.com', 'ROLE_USER')
ON CONFLICT (username) DO NOTHING;
