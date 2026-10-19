# Historical jobs2 packaging

The custom mini was used only in private v1.5.11/12-test. Starting with
v1.5.13-test Android draws the OpenCode chat and the official mini is opened
on demand. These manifest/installer templates preserve the previous binary's
provenance for reproducing the historical patch. They are not APK assets.
`scripts/package-opencode-mini.py` now writes into ignored
`.verification/opencode-mini-jobs2-archive/`; it does not add a frontend to the APK.
