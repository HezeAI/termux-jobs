# Private test10: official images and RPM update checks

This private candidate adds read-only official image discovery, separate from
Components and package upgrades. It does not add automatic upstream installation
or release migration. Package/updater state and all renderer/image pins remain
unchanged. Workspace schema8 and private checkpoint data remain compatible;
there are no data resets or migrations.

## Scope and review

- New official images shows upstream ARM64 release/snapshot metadata for the
  nine ordinary targets; Termux and Ubuntu AI remain excluded.
- Stable release selectors, installed branch comparisons and rolling-image
  comparisons are separate. Errors stay per-source; source pages are explicit.
- Only bounded metadata requests to fixed official hosts; default TLS remains
  enabled. The GHCR anonymous token stays in the request header to the same host
  and never appears in result JSON, receipts or logs.
- Sequential owned helpers have cancellation and time limits. Dismissing the
  window does not cancel. No live terminal, rootfs or unrelated process is used
  as a test fixture.
- Fedora uses a literal queryformat tab. openSUSE accepts NDB Packages.db and
  Index.db as well as SQLite and Berkeley DB, through guest-confined file reads.

## Verification

Results and final APK metadata are recorded in
`.verification/official-images-1441/`.

- 57 focused release JVM tests passed, including 10 updater UI/state checks.
- 46 Python checks passed: 16 new official-image/RPM cases, 25 existing update
  filesystem/workflow cases, 4 real owned-process cases and one real APT/dpkg
  transaction in a disposable repository fixture.
- The actual helper request entry point successfully checked all nine upstream
  ARM64 metadata sources. Its initially empty disposable PREFIX stayed empty.
- Read-only fingerprints of the phone's Fedora SQLite and openSUSE NDB package
  databases succeeded. No package manager or guest process was started for this
  read-only inspection, and no installed rootfs was altered.
- APK ZIP integrity, package/version/ABI, nondebuggable release signing identity,
  required runtime definitions, source assets and secret-pattern checks passed.
- Independent modern lint7.4.2 completed for all five modules: zero errors,
  225 existing warnings, no analysis-infrastructure failures. Final production
  source matches both the APK and lint verification stages.
- Comparison with test9: only two existing helper assets changed
  (`environment-updates.py`, `environment-update-packages.py`) and one new
  `environment-official-images.py` asset was added. All X11/renderer assets and
  all three JNI libraries remain byte-identical.

Final APK: `build/apk/Termux-Jobs-test10.apk`.

| Field | Value |
| --- | --- |
| Package | com.termux |
| Version | 2.0.10-test10 |
| versionCode | 1441 |
| ABI | arm64-v8a |
| Bytes | 39,889,470 |
| SHA-256 | 803a349c6f5f1f0a946f67a7ebaca2815347b615c390f7d999087e00ea10aeac |
| Signing certificate SHA-256 | dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644 |

The Python fixture suite covers official source selection, pre-release rejection,
Linux ARM64 selection, unsafe URLs/redirects, malformed metadata, partial failures,
cancellation, guest-confined NDB aliases and DNF queryformat. Existing update,
owned-process and real isolated APT/dpkg checks are rerun.

Real read-only network checks use the actual helper entry point with a disposable
empty PREFIX. They establish metadata availability at the recorded check time,
not future uptime or installed desktop compatibility.

## Device acceptance still required

Open Settings → Check environment updates → New official images. Check the
installed/upstream labels, source pages, Stop/retry and reopen behavior on the
phone. Repeat package checking for Fedora/openSUSE. Source fixtures and metadata
checks do not prove successful online DNF/Zypper upgrade transactions, which
remain subject to the test9 limitations.
