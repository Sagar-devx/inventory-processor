-- Flyway Migration V2: Create Inventory Items Table with UNIQUE(entry_code, batch_number)
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    entry_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
    expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_entry_batch UNIQUE (entry_code, batch_number)
);

CREATE INDEX IF NOT EXISTS idx_inv_item_name ON inventory_items (item_name);
CREATE INDEX IF NOT EXISTS idx_inv_entry_code ON inventory_items (entry_code);
CREATE INDEX IF NOT EXISTS idx_inv_batch_num ON inventory_items (batch_number);
CREATE INDEX IF NOT EXISTS idx_inv_status ON inventory_items (status);
CREATE INDEX IF NOT EXISTS idx_inv_expiry_date ON inventory_items (expiry_date);
