#!/bin/bash
BACKUP_DIR="./backups/$(date +%Y-%m-%d)"
mkdir -p "$BACKUP_DIR"

echo "🔐 Початок резервного копіювання..."

# 1. Бекап даних
cp input.txt "$BACKUP_DIR/library_data.txt"

# 2. Архівація логів (якщо вони є)
if [ -f "app.log" ]; then
    tar -czf "$BACKUP_DIR/logs.tar.gz" app.log
fi

# 3. Генерація контрольної суми
sha256sum "$BACKUP_DIR/library_data.txt" > "$BACKUP_DIR/checksum.txt"

echo "✅ Резервна копія створена у $BACKUP_DIR"