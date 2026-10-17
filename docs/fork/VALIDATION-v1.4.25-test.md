# v1.4.25-test validation

Tests:200-row preservation/order/status/truncation, reduced representation size, valid unsliced JSON beyond8000 characters; error/read/malformed-list fallback; shared coordinator delivering compact metadata while retaining original toolOutput/toolResult for multiple llama/LiteRT model identifiers. These use mocked generation and do not prove model compliance.

Reviewed selection before projection, raw saved history/UI path, checked location/access states and no permission/execution changes. Native payload unchanged fromv1.4.24. Device task/timing and list interpretation still pending.

Final evidence:661 passing cases,0 failures/errors/skips after updating two UI assertions for the renamed draft status. The full current-source run initially had only these two stale-label assertion failures; after assertion update, all39 LocalAiWorkspaceTest cases passed in a targeted rerun. full-suite evidence combines unchanged passing XML with this final class result; prior log retained as tests-before-label-assertion-update.log.
Release build/runtime gate passed:15920 definitions,30 reviewed unresolved types. Final ZIP CRC, package com.termux/versionCode1131/versionName1.4.25-test,arm64,existing certificate,staged source correspondence and limited secret scan passed. All native libraries match v1.4.24. SHA-256:`cc225309766dfc18cfa376e7e3ba213ec1fe48d32f4034030b37bcceee110fd3`. Delivery copy hash verified. Model behavior/latency remains pending device checks.
