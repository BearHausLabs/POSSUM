#!/bin/bash
# ============================================================================
# URSA JavaPOS Middleware - Linux Service Installer (systemd)
# ============================================================================
# Installs POSSUM as a systemd service.
# Must be run as root or with sudo.
#
# Prerequisites:
#   - POSSUM deployed at /opt/target/possum
#   - Java 17 installed
#   - devicestarter.sh deployed at /opt/target/possum/devicestarter.sh
# ============================================================================

set -e

SERVICE_NAME="ursa-pos-middleware"
SERVICE_FILE="${SERVICE_NAME}.service"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
POSSUM_HOME="/opt/target/possum"
LOG_PATH="/var/log/target/possum"

echo ""
echo "============================================================"
echo " URSA JavaPOS Middleware - Service Installer (Linux)"
echo "============================================================"
echo ""

# ---- Check for root privileges ----
if [ "$(id -u)" -ne 0 ]; then
    echo "ERROR: This script must be run as root or with sudo."
    echo "Usage: sudo $0"
    exit 1
fi

# ---- Verify prerequisites ----
if [ ! -f "${SCRIPT_DIR}/${SERVICE_FILE}" ]; then
    echo "ERROR: ${SERVICE_FILE} not found in ${SCRIPT_DIR}"
    exit 1
fi

if [ ! -d "${POSSUM_HOME}" ]; then
    echo "WARNING: ${POSSUM_HOME} does not exist. Creating it..."
    mkdir -p "${POSSUM_HOME}"
fi

if [ ! -f "${POSSUM_HOME}/devicestarter.sh" ]; then
    echo "WARNING: devicestarter.sh not found at ${POSSUM_HOME}/devicestarter.sh"
    echo "         Copy it there before starting the service."
fi

# ---- Create log directory ----
mkdir -p "${LOG_PATH}"
echo "Log directory: ${LOG_PATH}"

# ---- Make devicestarter.sh executable ----
if [ -f "${POSSUM_HOME}/devicestarter.sh" ]; then
    chmod +x "${POSSUM_HOME}/devicestarter.sh"
fi

# ---- Stop existing service if running ----
if systemctl is-active --quiet "${SERVICE_NAME}" 2>/dev/null; then
    echo "Stopping existing ${SERVICE_NAME} service..."
    systemctl stop "${SERVICE_NAME}"
fi

# ---- Copy service file ----
echo "Installing service file to /etc/systemd/system/${SERVICE_FILE}..."
cp "${SCRIPT_DIR}/${SERVICE_FILE}" "/etc/systemd/system/${SERVICE_FILE}"

# ---- Reload systemd ----
echo "Reloading systemd daemon..."
systemctl daemon-reload

# ---- Enable the service (auto-start on boot) ----
echo "Enabling service for auto-start..."
systemctl enable "${SERVICE_NAME}"

# ---- Start the service ----
echo "Starting ${SERVICE_NAME}..."
systemctl start "${SERVICE_NAME}"

# ---- Wait briefly and show status ----
sleep 2

echo ""
echo "============================================================"
echo " Installation Complete"
echo "============================================================"
echo ""
systemctl status "${SERVICE_NAME}" --no-pager
echo ""
echo " Service: ${SERVICE_NAME}"
echo ""
echo " Manage the service:"
echo "   sudo systemctl start   ${SERVICE_NAME}"
echo "   sudo systemctl stop    ${SERVICE_NAME}"
echo "   sudo systemctl restart ${SERVICE_NAME}"
echo "   systemctl status       ${SERVICE_NAME}"
echo ""
echo " View logs:"
echo "   journalctl -u ${SERVICE_NAME} -f"
echo "   tail -f ${LOG_PATH}/*.log"
echo ""
echo " POSSUM endpoint: http://localhost:8080"
echo " Health check:    http://localhost:8080/v1/health"
echo ""
