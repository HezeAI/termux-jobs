# v1.4.22-test validation

Changed paths inspected: profile copy/JSON/defaults, create-session/preset selection, existing-session save/reload, remote LOAD IPC and service CPU resolution. Busy-session and cancellation guards retained. Native libraries unchanged from v1.4.21.

Relevant tests cover persistence, missing-field defaults, invalid types/values, CPU-count clamping and the nested mode selector. Actual device menu and six-thread chat speed still require developer verification.

Final: 653 tests passed (0 failures/errors/skips), including 53 workspace/limits tests. Release build/runtime gate passed: 15917 definitions, 30 reviewed unresolved types. ZIP CRC, package/version/ABI/signature, source-stage comparison and limited secret scan passed. Both llama libraries match v1.4.21. SHA-256: `2fa3bd69138b676b4cf27aa9e3fe5dd1d0625cdbd8a5930fbd6ef81d89fa9a78`. APK copied to Download and hash rechecked.
