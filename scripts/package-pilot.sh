#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p release-assets
cp app/build/outputs/apk/debug/app-debug.apk release-assets/WoodMatch-2.0.2-pilot.apk
python3 - <<'PY'
import json, pathlib, subprocess, zipfile
root=pathlib.Path('.')
commit=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
config=root/'app/google-services.json'
manifest={'version':'2.0.2-pilot','applicationId':'com.example.kotlinapp.pilot','commit':commit,
          'firebaseConfigured':config.exists(),'apiConfigured':bool(__import__('os').environ.get('WOODMATCH_API_URL')),
          'signing':'Android debug certificate; testing only','deviceTests':'Pending physical-device validation'}
(root/'release-assets/build-info.json').write_text(json.dumps(manifest,indent=2)+'\n')
with zipfile.ZipFile('release-assets/woodmatch-review-assets.zip','w',zipfile.ZIP_DEFLATED) as output:
    for folder in ['app/src/main/res','stitch_woodmatch_seasonal_palette','docs']:
        for file in (root/folder).rglob('*'):
            if file.is_file(): output.write(file,str(file))
    for folder in ['app/build/test-results/testDebugUnitTest','app/build/reports']:
        for file in (root/folder).rglob('*'):
            if file.is_file(): output.write(file,str(file))
PY
(cd release-assets && sha256sum WoodMatch-2.0.2-pilot.apk woodmatch-review-assets.zip build-info.json > SHA256SUMS)
