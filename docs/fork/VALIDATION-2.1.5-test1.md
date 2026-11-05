# 2.1.5-test1 /1493 — private preparation, not issued

No APK has been issued for this version. No install, working restart, push or publication is authorized or performed.

## Subscription website links — Codex and Claude

The expanded status shows **Reset limit ↗** beside the subscription line, wrapping below the full name when width/font size requires it. The collapsed chip is unchanged. The explanation states that availability and conditions are on the website; the link is available even if native subscription data is missing.

Fixed destinations:

- Claude: https://claude.ai/settings/usage
- Codex: https://chatgpt.com/codex/settings/usage

Navigation reuses the existing external HTTPS/browser chooser. The shared browser function validates HTTPS and handles missing/restricted browser activity. It passes only the fixed URL, with no credentials or provider requests. There is no reset/purchase operation or return callback that alters counters. Native status polling remains unchanged; only fresh native data can change existing indicators.

This feature does **not** establish reset eligibility, subscription type, or availability of Claude five-hour/weekly windows. Those native fields and their actual chat route remain a separate investigation. The context meter stays separate. Clarification delivery/order is another independent task.

### Verification scope

Fresh JVM/UI checks cover both real expanded-menu entry points, fixed chooser destinations, unchanged synthetic current-request/status/limit values, RU/EN, 280dp with double font scale, wrapping full names, separate touch target, wider layout and reopened Claude menu. The existing documentation browser tests cover the shared browser path and rejected non-HTTPS addresses. Real browser/account actions are not performed.

Evidence: `.verification/test-2.1.5-test1/subscription-links-tests.log`, `subscription-first-results/`, `subscription-links-recheck.log`; modern Lint results are recorded separately when complete. The first wrapping assertions failed because Robolectric 4.4's font advances do not reproduce physical glyph widths, even in REALISTIC line-layout mode. The final fixture deliberately uses an overlong name that overflows in either renderer, retaining the 280dp width and double font scale. It verifies button relocation, full TextView text, unlimited label lines, no ellipsis and touch bounds. ShadowPaint measures String.length(); the API28 shadow also returned a zero line end for the nonempty label. These native-font layout results cannot verify physical multiline typography. Production layout thresholds remain unchanged. Actual short-label typography remains a phone check; preserved failed runs are not counted as passes.

Fresh modern Lint: all five modules analyzed, 0 errors /247 warnings. One additional ViewConstructor warning concerns this programmatic-only row, which is never inflated from XML. Report: `.verification/modern-lint-report/summary.json`; no Lint suppression or gate relaxation.

Across the targeted runs, **48 unique JVM/UI scenarios passed**, including the final layout-bound test. Original failed attempts remain preserved and are not counted as passes. Unchanged context/limit/menu tests from earlier targeted runs are reused only for the same production source bytes; four affected Java hashes match the modern-Lint stage. Compact identities/results: `.verification/test-2.1.5-test1/subscription-source-hashes.json` and `subscription-verification-summary.json`. The final focused layout run is `subscription-layout-checked.log`. These results do not close the separate timeline/controller regression check or any device check.

### Phone scenario — not checked

1. Open Codex status, then Claude status. Check the subscription line and explanation.
2. At large system font size, confirm the name remains complete and the button wraps without clipping.
3. Tap the link: verify the matching official host and usage page in the browser. No reset/purchase is required for this check.
4. Return to the chat. Confirm the agent continues and counters were not immediately reset by navigation.
5. Close and reopen the status. Missing data must remain missing until fresh native status arrives.

## Other pending changes

The four-agent timeline is not yet accepted: its controller scroll/session-switch check remains open. Claude clarification placement and subscription-window source verification remain independent. Final APK compilation/signature/package/version/runtime/native/template checks will apply to the actual new final file; old APK verification does not carry over.

Existing scope restrictions remain: attachment accumulation/disconnections are unresolved; low-level Android handoff/crash/device-backup checks and real Gemini API/Vertex login remain unverified; MCP/plugins excluded.
