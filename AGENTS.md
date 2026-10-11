# Workspace boundaries

The user requires all fork development and generated files to stay in
`/root/termux-fork`. Do not change installed Termux, its data, the shared Android
SDK, or the Ubuntu environment. Do not install APKs, replace apps, start/stop the
user's terminal sessions, or install system packages.

Existing tools and SDK components may be read. Put any additional tools, caches,
SDK components and build outputs inside this repository, in ignored `.tools/`
or `.verification/` directories. Use command-local environment variables only.
Use a local SDK copy for Gradle so that automatic package installation cannot
write to the shared SDK. Verification on an installed app requires separate user
instructions; source edits and isolated builds are authorized.
