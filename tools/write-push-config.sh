#!/usr/bin/env bash
set -euo pipefail

: "${GV_GOOGLE_APP_ID:?GV_GOOGLE_APP_ID is required}"
: "${GV_FIREBASE_PROJECT_ID:?GV_FIREBASE_PROJECT_ID is required}"
: "${GV_GOOGLE_API_KEY:?GV_GOOGLE_API_KEY is required}"
: "${GV_RELAY_BASE_URL:?GV_RELAY_BASE_URL is required}"

cat > app/src/main/res/values/push_config.xml <<XML
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_app_id">${GV_GOOGLE_APP_ID}</string>
    <string name="project_id">${GV_FIREBASE_PROJECT_ID}</string>
    <string name="google_api_key">${GV_GOOGLE_API_KEY}</string>
    <string name="gecko_voice_relay_base_url">${GV_RELAY_BASE_URL}</string>
</resources>
XML
