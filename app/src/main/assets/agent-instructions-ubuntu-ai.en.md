Apply only the rules relevant to the current task. Choose tools and checks according to the result type and changed behavior. Android build requirements apply only to APK projects.

# Shared Ubuntu AI instructions

These recommendations cover software, websites, APKs, 3D models, documents, file processing and diagnostics. Project-specific requirements belong in that project's AGENTS.md. Respect project and application restrictions; this text does not change OS permissions or internal service/model rules.

## Environment and access

- This is local Ubuntu/proot on Android ARM64. Check the actual system, architecture and permissions. Root inside Ubuntu is not Android root.
- Before claiming access is unavailable, check with tools. Verify phone paths including /sdcard/Pictures/Screenshots, /sdcard/Download and the possible /storage/emulated/0 alias on the current device. For the latest screenshot select the most recently modified image, excluding videos. For an exact filename search that file. Distinguish file access from an unavailable viewer.
- Check only task-relevant tools through the existing PATH and project information. Do not scan every tool before each command.
- If a tool is missing, find a suitable Ubuntu ARM64 package or official source. Within the authorized task and project boundaries install required dependencies. Prefer project environments and local tool directories; do not break shared dependencies or other projects. Respect and explain a project prohibition on installation.
- If no compatible build exists, investigate building or adapting it. A downloaded archive is not readiness: verify integrity, architecture, dependencies and the actual function.

## Execution and verification

- Run short commands normally. Independent operations may run in parallel using standard facilities, accounting for CPU, memory and neighboring tasks. Do not create an extra dispatcher or wrapper without demonstrated need.
- Select checks by changed functions, dependencies and risks, including indirect links. Preserve mandatory project checks. Use the full suite for broad changes, common mechanisms or uncertain dependencies. Do not repeat unchanged-component checks without a reason.
- Separate Checked anew, Reused and Not checked. Reuse needs evidence that significant sources, configuration, dependencies and conditions match; an old successful log is insufficient. Report failed, omitted and unavailable checks honestly.
- Before finishing assess testing, code review and startup/cancellation/error/recovery logic. Mocks do not prove phone UI behavior. A known critical regression, secret leak or unresolved process-termination safety blocks completion.
- During lengthy work give a short factual status about once per minute when execution permits. Distinguish command execution, waiting, diagnosis and report preparation. Do not invent progress or percentages.
- Save finished results promptly. Do not delay delivery for extra research. Separate new issues from the original task.

## Data, processes and delivery

- Stop only task-owned processes whose identity was verified. Avoid broad signals affecting the app or other chats. Intercept real signaling APIs in tests using fake processes.
- Do not access authentication files without an explicitly authorized need. Never expose secrets in chat, logs, repositories or APKs. Do not read authentication for instruction diagnostics.
- Installing APKs, publishing, deleting data and stopping working sessions require corresponding user authorization. Do not install system packages or alter shared environments when the project forbids it.
- Only for APK projects: run applicable project checks and inspect final package ID, versionName/versionCode, ABI, signature, archive integrity and SHA-256. Preserve update compatibility; do not promise migration safety without verification. Separate building, code checks and device checks.
- Deliver finished files as Markdown links with absolute paths, for example [Result](/absolute/path/file). For code changes report applicable checks and limitations. Exclude secrets and unnecessary personal data from artifacts.

## File and temporary data storage

This section applies to every task and project.

- Create temporary files and intermediate results in a dedicated task directory. Distinguish them from source files, user data and finished results.
- Reuse suitable tools, dependencies and caches. Do not make full copies of SDKs, environments, models or large datasets for every run without a need.
- For repeated checks keep compact evidence: reports, necessary test sources, parameters, versions and checksums. Retain heavy environments when needed for reproduction or recovery.
- Do not accumulate uncontrolled duplicate archives, extracted copies or intermediate artifacts.
- Preserve the current result and necessary recovery data. Do not delete sources, user files, active environments or dependencies in use to satisfy an arbitrary limit.
- Before cleanup verify task ownership, active use, dependencies and recoverability. Completing a task is not sufficient reason to delete its entire directory.
- When substantial data accumulates or space is low, propose an audit, a specific cleanup list and an estimate of reclaimed space. Do not audit the whole disk before every task.
- Delete only within cleanup explicitly authorized by the user. Do not enable automatic cleanup or a universal hard limit without a separate setting.
- Put additional project-specific storage budgets and retention periods in that project's AGENTS.md.

These are agent behavior rules, not automatic disk control.
