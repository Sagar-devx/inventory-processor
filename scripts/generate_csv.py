#!/usr/bin/env python3
"""
Generates a realistic inventory CSV dataset of 10,000 records:
- ~9,700 Valid rows
- ~200 Invalid rows (negative prices, missing fields, invalid date formats)
- ~100 Duplicate (entry_code, batch_number) pairs to test deduplication
"""

import os
import random
from datetime import datetime, timedelta

SAMPLE_DIR = os.path.join(os.path.dirname(__file__), "..", "sample-data")
OUTPUT_FILE = os.path.join(SAMPLE_DIR, "inventory_10k.csv")

MEDICINES = [
    ("Paracetamol 500mg", "PARA"),
    ("Amoxicillin 250mg", "AMOX"),
    ("Ibuprofen 400mg", "IBUP"),
    ("Metformin 500mg", "METF"),
    ("Atorvastatin 20mg", "ATOR"),
    ("Omeprazole 20mg", "OMEP"),
    ("Azithromycin 500mg", "AZIT"),
    ("Cetirizine 10mg", "CETI"),
    ("Pantoprazole 40mg", "PANT"),
    ("Amlodipine 5mg", "AMLO"),
    ("Losartan 50mg", "LOSA"),
    ("Ciprofloxacin 500mg", "CIPR"),
    ("Levothyroxine 50mcg", "LEVO"),
    ("Doxycycline 100mg", "DOXY"),
    ("Metoprolol 25mg", "METO"),
]

def generate():
    os.makedirs(SAMPLE_DIR, exist_ok=True)
    random.seed(42)

    total_records = 10000
    duplicate_target = 100
    invalid_target = 200
    valid_target = total_records - duplicate_target - invalid_target  # 9,700

    rows = []
    seen_pairs = []

    start_date = datetime(2025, 6, 1)

    print(f"Generating {valid_target} valid records...")
    # 1. Valid records
    for i in range(1, valid_target + 1):
        med_name, prefix = random.choice(MEDICINES)
        code_num = (i % 2000) + 1
        entry_code = f"MED-{prefix}-{code_num:04d}"
        batch_number = f"BAT-{prefix}-{(i // 50) + 1:04d}-{random.randint(10, 99)}"
        quantity = random.randint(10, 500)
        unit_price = round(random.uniform(5.50, 450.00), 2)
        expiry_date = (start_date + timedelta(days=random.randint(60, 1000))).strftime("%Y-%m-%d")

        rows.append((entry_code, med_name, batch_number, str(quantity), f"{unit_price:.2f}", expiry_date))
        seen_pairs.append((entry_code, med_name, batch_number, quantity, unit_price, expiry_date))

    print(f"Generating {invalid_target} invalid records...")
    # 2. Invalid records (various validation errors)
    for i in range(invalid_target):
        error_type = i % 5
        med_name, prefix = random.choice(MEDICINES)
        entry_code = f"MED-ERR-{i:04d}"
        batch_number = f"BAT-ERR-{i:04d}"
        quantity = random.randint(10, 200)
        unit_price = round(random.uniform(10.0, 100.0), 2)
        expiry_date = "2026-12-31"

        if error_type == 0:
            # Negative price
            unit_price_str = "-15.00"
            rows.append((entry_code, med_name, batch_number, str(quantity), unit_price_str, expiry_date))
        elif error_type == 1:
            # Negative quantity
            qty_str = "-50"
            rows.append((entry_code, med_name, batch_number, qty_str, f"{unit_price:.2f}", expiry_date))
        elif error_type == 2:
            # Missing entry code
            rows.append(("", med_name, batch_number, str(quantity), f"{unit_price:.2f}", expiry_date))
        elif error_type == 3:
            # Invalid date format
            rows.append((entry_code, med_name, batch_number, str(quantity), f"{unit_price:.2f}", "31/12/2026"))
        elif error_type == 4:
            # Non-numeric price
            rows.append((entry_code, med_name, batch_number, str(quantity), "INVALID_PRICE", expiry_date))

    print(f"Generating {duplicate_target} duplicate records...")
    # 3. Duplicate records (identical entry_code and batch_number as seen_pairs)
    for i in range(duplicate_target):
        orig = seen_pairs[i * (len(seen_pairs) // duplicate_target)]
        # Re-use entry_code and batch_number
        rows.append((orig[0], orig[1], orig[2], str(orig[3]), f"{orig[4]:.2f}", orig[5]))

    # Shuffle to intersperse duplicates and errors realistically
    random.shuffle(rows)

    print(f"Writing {len(rows)} records to {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("entry_code,item_name,batch_number,quantity,unit_price,expiry_date\n")
        for row in rows:
            f.write(f"{row[0]},{row[1]},{row[2]},{row[3]},{row[4]},{row[5]}\n")

    file_size_mb = os.path.getsize(OUTPUT_FILE) / (1024 * 1024)
    print(f"Dataset generated successfully! File size: {file_size_mb:.2f} MB")
    print(f"Total: {len(rows)} | Valid: {valid_target} | Invalid: {invalid_target} | Duplicates: {duplicate_target}")

if __name__ == "__main__":
    generate()
