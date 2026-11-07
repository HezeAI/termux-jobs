# Optional old-interface rollback for v2.1.5

Version 2.1.5-rollback, versionCode 1521, com.termux, existing release certificate.
Source is the accepted legacy interface at dfbf111f (the source of standby1499).
Only version metadata and this documentation change. Existing standby APKs are untouched.

Manual installation only. The stable update.json points to the new-interface 1520 APK.
Before rollback, disable Auto/Bypass in every saved Claude profile and save an ordinary
mode. The legacy validator does not accept those new mode values. This APK is not a
backup of user data. Actual on-device downgrade/data retention is not verified.
No data reset or environment reinstall is required by this build; do not uninstall.
After installing code1521, the code1520 main APK cannot update it normally. A future
main build must have a code above1521. The release contains source tags for both APKs.
