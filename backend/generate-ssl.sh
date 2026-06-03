#!/bin/sh
set -e

SSL_DIR="$(dirname "$0")/ssl"
mkdir -p "$SSL_DIR"

if [ -f "$SSL_DIR/cert.pem" ] && [ -f "$SSL_DIR/key.pem" ]; then
    echo "SSL certificates already exist in $SSL_DIR"
    echo "Delete them first if you want to regenerate."
    exit 0
fi

echo "Generating self-signed SSL certificate..."
openssl req -x509 -newkey rsa:2048 \
    -keyout "$SSL_DIR/key.pem" \
    -out "$SSL_DIR/cert.pem" \
    -days 365 -nodes \
    -subj "/CN=2.25.142.139/O=AppEnglish/C=AR"

echo "SSL certificates generated in $SSL_DIR/"
echo "  cert.pem - Certificate"
echo "  key.pem  - Private key"
echo ""
echo "NOTE: Browsers will show a security warning for self-signed certificates."
echo "Click 'Advanced' and 'Proceed' to continue."
