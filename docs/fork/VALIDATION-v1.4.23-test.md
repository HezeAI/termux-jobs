# v1.4.23-test validation

Inspected: fitted-context measurement, isolated tokenizer probes, metadata callback via existing service/remote path, per-pass report lifecycle and bounded saved reports. No native source changes; both llama libraries must match v1.4.22. No tools, permissions or model context modified by measurement.

Tests cover attribution classification, preservation of source history, absence of text in metadata, LiteRT estimate labelling, completed-pass persistence and reset. Final build/artifact evidence appended below. Device/Qwen breakdown not yet verified.

Additional guard: attribution errors yield metadata-only unavailable status and do not abort inference; cancellation is propagated. Tested with a throwing counter and cancellation.

Final evidence: 656 tests passed, no failures/errors/skips (53 workspace/limits tests included). Final release build and runtime gate passed:15919 definitions,30 reviewed unresolved types. ZIP CRC, package com.termux, versionName1.4.23-test/versionCode1129, arm64 ABI, existing certificate, staged sources and limited secret scan passed. Native payload unchanged from v1.4.22. SHA-256: `b197ae456a9cd92bd7c7e061ec653658c5460f9f79dbb687c1743ab9a08a5f12`. Delivery copy hash rechecked.
