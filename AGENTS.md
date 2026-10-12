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

# Release documentation

The user requires documentation to be reviewed and updated with every GitHub
release, including README.md. Follow docs/fork/RELEASING.md. Update the changelog,
release notes, validation evidence, version and compatibility information, and
any usage/design/architecture documents affected by the final implementation.
Keep historical reports clearly marked and never claim unperformed checks.
This requirement does not independently authorize publishing a new release.
