#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${FIREBASE_PROJECT:?Define FIREBASE_PROJECT con el proyecto Firebase de pruebas}"
if [[ "$FIREBASE_PROJECT" == demo-* ]]; then
  echo 'Este script despliega a un proyecto real de pruebas; usa emulators:exec para demo-*.' >&2
  exit 1
fi
if [[ ! -f app/google-services.json ]]; then
  echo 'Falta app/google-services.json. Registra primero la aplicación Android en el proyecto de pruebas.' >&2
  exit 1
fi
node -e 'const fs = require("node:fs"); const config = JSON.parse(fs.readFileSync("app/google-services.json", "utf8")); if (config.project_info?.project_id !== process.env.FIREBASE_PROJECT) { console.error("El JSON Android no corresponde a FIREBASE_PROJECT."); process.exit(1); }'
if [[ "$(node -p 'process.versions.node.split(".")[0]')" != 22 ]]; then
  echo 'Activa Node 22 antes de desplegar las funciones.' >&2
  exit 1
fi
npm ci --prefix functions
node --test functions/auth.test.js functions/policy.test.js
functions/node_modules/.bin/firebase deploy --project "$FIREBASE_PROJECT" --only firestore:rules,storage,functions:woodmatch
