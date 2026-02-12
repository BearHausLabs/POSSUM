#!/bin/bash
# ============================================================================
# URSA JavaPOS Middleware - Linux Service Uninstaller (systemd)
# ============================================================================
# Stops, disables, and removes the POSSUM systemd service.
# Must be run as root or with sudo.
# ============================================================================

set -e

SERVICE_NAME="ursa-pos-middleware"
SERVICE_FILE="${SERVICE_NAME}.service"

echo ""
echo "============================================================"
echo " URSA JavaPOS Middleware - Service Uninstaller (Linux)"
echo "============================================================"
echo ""

# ---- Check for root privileges ----
if [ "$(id -u)" -ne 0 ]; then
    echo "ERROR: This script must be run as root or with sudo."
    echo "Usage: sudo $0"
    exit 1
fi

# ---- Check if service exists ----
if [ ! -f "/etc/systemd/system/${SERVICE_FILE}" ]; then
    echo "Service ${SERVICE_NAME} is not installed. Nothing to do."
    exit 0
fi

# ---- Stop the service if running ----
if systemctl is-active --quiet "${SERVICE_NAME}" 2>/dev/null; then
    echo "Stopping ${SERVICE_NAME}..."
    systemctl stop "${SERVICE_NAME}"
    echo "Service stopped."
else
    echo "Service is not running."
fi

# ---- Disable the service ----
echo "Disabling service..."
systemctl disable "${SERVICE_NAME}" 2>/dev/null || true

# ---- Remove the service file ----
echo "Removing /etc/systemd/system/${SERVICE_FILE}..."
rm -f "/etc/systemd/system/${SERVICE_FILE}"

# ---- Reload systemd ----
echo "Reloading systemd daemon..."
systemctl daemon-reload
systemctl reset-failed 2>/dev/null || true

echo ""
echo "============================================================"
echo " Service '${SERVICE_NAME}' has been removed successfully."
echo "============================================================"
echo ""
echo " Log files remain at /var/log/target/possum/ (delete manually if desired)."
echo " Application files remain at /opt/target/possum/ (delete manually if desired)."
echo ""
