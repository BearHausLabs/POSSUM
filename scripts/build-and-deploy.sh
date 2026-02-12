#!/bin/bash
# ============================================================================
# URSA JavaPOS Middleware - Build and Deploy Script (Linux)
# ============================================================================
# Builds POSSUM from source and deploys to /opt/target/possum.
# Run this BEFORE install-service.sh on a new register.
#
# Usage:
#   ./build-and-deploy.sh              (build + deploy)
#   ./build-and-deploy.sh --skip-build (deploy only, use existing build)
#
# Prerequisites:
#   - Java 17 installed
#   - Git repo cloned with source code
#   - Must run as root or with sudo for deployment to /opt
# ============================================================================

set -e

# ---- Configuration ----
POSSUM_HOME="/opt/target/possum"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# ---- Parse arguments ----
SKIP_BUILD=0
if [ "$1" = "--skip-build" ]; then
    SKIP_BUILD=1
fi

echo ""
echo "============================================================"
echo " URSA JavaPOS Middleware - Build and Deploy"
echo "============================================================"
echo ""
echo " Repository:  $REPO_DIR"
echo " Deploy to:   $POSSUM_HOME"
echo " Skip build:  $SKIP_BUILD"
echo ""

# ---- Check for root ----
if [ "$(id -u)" -ne 0 ]; then
    echo "WARNING: Not running as root. Deployment to $POSSUM_HOME may fail."
    echo "         Consider running with sudo."
    echo ""
fi

# ---- Verify Java ----
if ! command -v java &>/dev/null; then
    echo "ERROR: Java not found on PATH. Install Java 17 first."
    exit 1
fi

echo "Java version:"
java -version 2>&1 | head -1
echo ""

# ---- Build ----
if [ $SKIP_BUILD -eq 0 ]; then
    echo ""
    echo "[1/3] Building POSSUM..."
    echo "============================================================"

    if [ ! -f "$REPO_DIR/gradlew" ]; then
        echo "ERROR: gradlew not found in $REPO_DIR"
        echo "Are you in the POSSUM repository?"
        exit 1
    fi

    cd "$REPO_DIR"
    chmod +x gradlew
    ./gradlew clean bootJar -x test

    echo ""
    echo "Build successful."
else
    echo ""
    echo "[1/3] Skipping build (--skip-build)"
fi

# ---- Find the built JAR ----
JAR_FILE=""
for f in "$REPO_DIR"/build/libs/PossumDeviceManager-*.jar; do
    # Skip plain JARs
    if [[ "$f" != *"plain"* ]]; then
        JAR_FILE="$f"
    fi
done

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: Could not find built JAR in $REPO_DIR/build/libs/"
    echo "Run the build first, or check for build errors."
    exit 1
fi

echo ""
echo " Found JAR: $JAR_FILE"

# ---- Create deployment directories ----
echo ""
echo "[2/3] Creating deployment directories..."
echo "============================================================"

mkdir -p "$POSSUM_HOME"
mkdir -p "$POSSUM_HOME/config"
mkdir -p "$POSSUM_HOME/logs"
mkdir -p "$POSSUM_HOME/externalLib"
mkdir -p /var/log/target/possum/CrashLog

# ---- Deploy files ----
echo ""
echo "[3/3] Deploying to $POSSUM_HOME..."
echo "============================================================"

# Copy JAR (renamed to possum.jar)
echo " Copying possum.jar..."
cp -f "$JAR_FILE" "$POSSUM_HOME/possum.jar"

# Also keep the original name for reference
cp -f "$JAR_FILE" "$POSSUM_HOME/$(basename "$JAR_FILE")"

# Copy config files
echo " Copying config files..."
[ -f "$REPO_DIR/src/main/resources/devcon.xml" ] && cp -f "$REPO_DIR/src/main/resources/devcon.xml" "$POSSUM_HOME/devcon.xml"
[ -f "$REPO_DIR/src/main/resources/ECIEncoding.csv" ] && cp -f "$REPO_DIR/src/main/resources/ECIEncoding.csv" "$POSSUM_HOME/ECIEncoding.csv"
[ -f "$REPO_DIR/src/main/resources/LabelIdentifiers.csv" ] && cp -f "$REPO_DIR/src/main/resources/LabelIdentifiers.csv" "$POSSUM_HOME/LabelIdentifiers.csv"
[ -f "$REPO_DIR/src/main/resources/IHSParser.csv" ] && cp -f "$REPO_DIR/src/main/resources/IHSParser.csv" "$POSSUM_HOME/IHSParser.csv"
[ -f "$REPO_DIR/src/main/resources/logback-spring.xml" ] && cp -f "$REPO_DIR/src/main/resources/logback-spring.xml" "$POSSUM_HOME/logback-spring.xml"
[ -f "$REPO_DIR/src/main/resources/application.properties" ] && cp -f "$REPO_DIR/src/main/resources/application.properties" "$POSSUM_HOME/application.properties"

# Copy possum-config.yml only if one doesn't already exist
if [ -f "$REPO_DIR/src/main/resources/possum-config.yml" ]; then
    if [ ! -f "$POSSUM_HOME/possum-config.yml" ]; then
        cp -f "$REPO_DIR/src/main/resources/possum-config.yml" "$POSSUM_HOME/possum-config.yml"
        echo " Copied default possum-config.yml (new install)"
    else
        echo " Keeping existing possum-config.yml (not overwritten)"
    fi
fi

# Copy service scripts
echo " Copying service scripts..."
cp -f "$SCRIPT_DIR/devicestarter.sh" "$POSSUM_HOME/devicestarter.sh"
chmod +x "$POSSUM_HOME/devicestarter.sh"

cp -f "$SCRIPT_DIR/install-service.sh" "$POSSUM_HOME/install-service.sh"
chmod +x "$POSSUM_HOME/install-service.sh"

cp -f "$SCRIPT_DIR/uninstall-service.sh" "$POSSUM_HOME/uninstall-service.sh"
chmod +x "$POSSUM_HOME/uninstall-service.sh"

# Copy systemd unit file
if [ -f "$SCRIPT_DIR/ursa-pos-middleware.service" ]; then
    cp -f "$SCRIPT_DIR/ursa-pos-middleware.service" "$POSSUM_HOME/ursa-pos-middleware.service"
fi

echo ""
echo "============================================================"
echo " Build and Deploy Complete!"
echo "============================================================"
echo ""
echo " Deployed to: $POSSUM_HOME"
echo ""
echo " Directory contents:"
echo "   possum.jar              - Application JAR"
echo "   devcon.xml              - JavaPOS device configuration"
echo "   devicestarter.sh        - Service runner script"
echo "   install-service.sh      - Systemd service installer"
echo "   uninstall-service.sh    - Systemd service uninstaller"
echo "   ursa-pos-middleware.service - Systemd unit file"
echo ""
echo " Next steps:"
echo "   1. Run: sudo $POSSUM_HOME/install-service.sh"
echo "   2. Verify: curl http://localhost:8080/v1/health"
echo "   3. Discovery: curl http://localhost:8080/v1/discovery"
echo ""
