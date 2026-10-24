#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -ne 3 || "$1" != "--apply" ]]; then
  echo 'Preparado para revisión. No se desplegó ningún servicio.'
  echo 'Después de configurar cuentas gratuitas y revisar docs/PILOTO.md:'
  echo 'bash scripts/deploy-pilot.sh --apply FIREBASE_PROJECT_ID SUPABASE_PROJECT_REF'
  exit 0
fi
firebase_project="$2"
supabase_project="$3"
[[ "$firebase_project" =~ ^[a-z][a-z0-9-]{4,28}[a-z0-9]$ ]] || { echo 'Proyecto Firebase no válido'; exit 1; }
[[ "$supabase_project" =~ ^[a-z]{20}$ ]] || { echo 'Referencia Supabase no válida'; exit 1; }
command -v firebase >/dev/null
command -v supabase >/dev/null
# No billing, Storage, Cloud Functions, or Play Store operations.
firebase deploy --project "$firebase_project" --only firestore:rules,firestore:indexes
supabase link --project-ref "$supabase_project"
supabase db push --linked
supabase functions deploy woodmatch --project-ref "$supabase_project" --no-verify-jwt
echo 'Servicios desplegados. Completa las pruebas con dos cuentas antes de distribuir el APK configurado.'
