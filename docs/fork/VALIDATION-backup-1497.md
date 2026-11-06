# Standby1497 — accepted old interface

Requested2026-11-06 for primary1496. Source baseline11aa991a (accepted UI83c233ef).
Only version metadata changes to2.1.5-backup2/code1497. No redesign code.
1493/1494/1495 artifacts remain immutable. No install, push or publication.
This is an application binary backup, not a backup of user data; future data rollback is unverified.
Native and packaged-source input inventories match1495. Excluded generated Python bytecode is not packaged.
Checked anew: release build, final ZIP/CRC and outer ZIP records; com.termux,
2.1.5-backup2/code1497, ARM64, non-debuggable; v2 signature and unchanged certificate.
Static runtime: 14312 definitions, requiredMissing=[],31 reviewed optional references.
All124 assets and resources.arsc equal1495; all seven ELF equal stablev2.1.4.
588 production sources match the build stage.95 native and602 non-generated
build inputs match1495 (version metadata is the intentional subsequent change).
The old1495 functional checks and five-module Lint0errors/250warnings are reused
for identical source/resource/dependency inputs; they were not rerun for1497.
Phone acceptance belongs to the old1493 UI and user-reported scenarios, not a new1497 install.

APK: `/sdcard/Download/Termux-Jobs-v2.1.5-backup2-1497-arm64.apk`,40533186bytes.
SHA256:`749c56226b9d58953edad8cb30a3eab30510253d9637e2d2655ba3768de6e3f7`.
Download and /storage/emulated/0 alias copies were read back and match exactly.
Evidence: `.verification/backup1497-source/.verification/backup-2.1.5-1497/` in the main checkout. Standby source commit:269a63cf.
The initial setup attempt failed on generated Python bytecode in the old inventory;
its log is retained. The successful preparation explicitly excludes only bytecode
already excluded by the build staging script.

1497 is standby: do not install in advance if planning to install main1496.
Future data, external CLI/Components and emergency restoration are not verified.
No install, session restart, tag, push or publication was performed.
