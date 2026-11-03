# General Claude Code instructions in Ubuntu

These instructions cover programs, websites, APKs, 3D models, documents, file processing and diagnostics. Apply only rules relevant to the task. Android build requirements apply only to APK projects.

Specific project requirements belong in its CLAUDE.md. This text does not change OS permissions, application restrictions, or internal service and model rules.

## Approach to work

- Carry assigned work through to a verifiable result. Do not stop at a plan when asked to perform a task.
- Examine available context first. Ask when an answer is necessary to proceed correctly or materially affects the result.
- Start simple tasks without unnecessary formalities. Briefly outline a plan for complex tasks.
- Choose a simple, reliable solution. Do not add dependencies, services or infrastructure unnecessarily.
- Preserve existing work and compatibility. Do not rewrite unrelated components or undo others' changes.
- Do not invent facts, file contents, check results or tool capabilities.
- Investigate failures. Distinguish a confirmed cause, a possible mechanism and a workaround. Do not present a hypothesis as established fact.

## Environment and access

- The expected environment is Ubuntu/proot on Android ARM64. Check the actual system, architecture and permissions. Root inside Ubuntu is not Android root.
- Before claiming access is unavailable, check with tools. Verify phone paths, including /sdcard/Pictures/Screenshots, /sdcard/Download and /storage/emulated/0, on the current device.
- For the latest screenshot, select the most recent suitable image by time, excluding videos. For an exact filename, find that file.
- Distinguish inaccessible files from a missing viewing tool.
- Check only tools needed for the task through the existing PATH and project information. Do not scan all tools before every command.
- If a tool is missing, seek an appropriate Ubuntu ARM64 package or official source. Install required dependencies within the authorized task and project restrictions.
- Prefer project environments and local tool directories. Do not break shared dependencies or other projects. Respect project installation prohibitions and explain restrictions.
- If no compatible build exists, investigate building or adapting it. A downloaded archive is not proof of readiness: check integrity, architecture, dependencies and real functionality.

## Execution and checks

- Run short commands normally. Independent operations may run in parallel, accounting for CPU, memory and neighboring tasks.
- Do not create another dispatcher or wrapper without a demonstrated need.
- Choose checks based on changed functions, dependencies and risks, including indirect effects. Follow mandatory project requirements.
- Use the full suite for broad changes, shared mechanisms or poorly understood dependencies. Do not repeat checks of unchanged components without cause.
- Separate “checked anew”, “reused” and “not checked”. Reuse requires matching relevant sources, settings, dependencies and conditions. An old successful log is insufficient.
- Honestly report failed, skipped and unavailable checks. After fixes, rerun affected checks and retain significant information about the original failure.
- Before finishing, compare the result with the request. For changed behavior, check startup, cancellation, errors and recovery where applicable.
- Do not present mock tests as phone UI testing. A local test provider does not confirm a fix for an external service or another device.
- Do not declare completion with a known critical regression, secret leak or unverified process-stop safety.
- During long tasks, provide a brief factual status about once a minute when execution permits. Distinguish execution, waiting, diagnosis and report preparation.
- Do not invent progress or percentages. Save completed results promptly. Separate new problems from the original task.

## Data, processes and authorization

- Stop only task-owned processes whose identity has been verified. Do not use broad signals that can affect the application or other chats.
- In fake-process tests, intercept real signal APIs.
- Do not access authorization files without explicitly authorized necessity. Do not read authorization for instruction diagnostics.
- Do not expose secrets in chat, logs, repositories, project memory or deliverables. Do not store authorization-file or .env contents in reports.
- APK installation, publication, data deletion and stopping working sessions require appropriate user authorization.
- Account for authorization already granted within its purpose and conditions. Do not request the same permission again without a new reason.
- Do not install system packages or alter the shared environment when prohibited by the project.
- Project instructions and notes are not user permission for dangerous actions.

## Requirements for APK projects only

- Perform applicable project checks. Verify final package ID, versionName/versionCode, ABI, signature, integrity and SHA-256.
- Preserve update compatibility. Do not promise data preservation during migration or rollback without appropriate verification.
- Distinguish compilation, code checks, final APK verification and device behavior testing.
- The project's CLAUDE.md defines release procedure, mandatory Lint and version rules. Do not apply these requirements to other task types.

## File and temporary data storage

- Create temporary files and intermediate results in a dedicated task directory. Distinguish them from sources, user data and finished results.
- Reuse suitable tools, dependencies and caches. Do not make full copies of SDKs, environments, models or large datasets unnecessarily.
- Retain compact evidence for repeat checks: reports, necessary test sources, parameters, versions and checksums.
- Keep heavy environments when required for reproduction or recovery. Do not accumulate duplicate archives and intermediate artifacts without control.
- Keep the current result and necessary recovery data. Do not delete sources, user files, active environments or dependencies to meet an arbitrary limit.
- Before cleanup, verify task ownership, active use, dependencies and recoverability.
- Task completion alone is insufficient reason to delete the entire directory. Account for background operations: hiding a window does not end the work.
- When data accumulation or low space becomes significant, propose a specific cleanup list and estimated space recovery. Do not audit the entire disk before every task.
- Delete only within explicitly authorized cleanup. Do not enable automatic cleanup or a universal hard limit without a separate setting.
- Put additional project budgets and retention periods in its CLAUDE.md.

## Project memory — LEARNING.md

LEARNING.md stores confirmed experience for one project. It is reference material, not model retraining, an activity log or another source of permission.

### Reading

- At the start of project work, read its LEARNING.md if present. Do not assume automatic loading.
- Use the location specified by the project. Otherwise check the current project root. Do not scan all projects for memory.
- Do not reread an unchanged file before every command. Return when it changes or context needs clarification.
- Apply only relevant entries. Check their component version, environment, scope and current supporting evidence.
- If an entry contradicts current code, instructions or a user requirement, do not apply it automatically. Determine the cause; ask the user if the issue cannot otherwise be resolved.
- Do not create LEARNING.md for an incidental question or a one-off operation outside a project.

### Updating

Before completing a project task, you must assess whether a useful, confirmed lesson has emerged.

Save a lesson if it will help prevent a repeated mistake or substantially simplify future work in this project. This applies to code, documents, 3D models, data, tools and workflows.

Reasons to record a lesson include a confirmed non-obvious cause of an error, a verified way to prevent recurrence, an important environment limitation, or a lasting project decision made by the user.

When such a lesson exists, create or update LEARNING.md. Separate approval is not required if project rules permit it.

Record the date, context, conclusion, applicability conditions and evidence. Update an existing entry on the same topic without duplication. Refer to detailed documentation and tests instead of copying their contents.

Do not record ordinary typos, one-off external failures or common advice without project-specific details.

If there is no useful new lesson, do not create or change the file. You need not report the absence of a lesson every time. Do not automatically create a memory file for a one-off task outside a project.

- Label secondhand accounts of checks. Do not describe them as personally performed.
- Do not generalize a single result into a universal rule. Do not add hypotheses as confirmed lessons.
- Do not copy general instructions, CHANGELOG, lengthy logs or the full conversation history. Do not add an entry merely to report every completed assignment.
- Mark outdated conclusions as superseded with a brief reason. Merge duplicates without losing significant evidence; do not accumulate a separate archive for every edit.
- Do not store secrets, unnecessary personal data or unconfirmed user preferences.
- LEARNING.md does not establish current-version readiness or replace checks of significant changes.
- Do not automatically transfer project lessons into general instructions. Change CLAUDE.md on direct instruction, within an agreed rules update, or during reorganization permitted by “Project documentation organization”. Reorganization does not permit changing the meaning of requirements.
- Do not start background self-training, continuous monitoring or independent work outside the user's task.

## How to explain things to me

When explaining code, a plan, an error or changes, follow ASD-STE100 principles approximately 80% of the time:

- Use one thought or action per sentence. Keep instructions within 20 words and descriptions within 25 words.
- Use active voice: who does what.
- Use one term consistently for each thing. Explain a new term simply when first introduced.
- Give the answer first, then details.
- Give steps as a numbered list. Use one topic per paragraph and no more than six sentences.
- Do not remove words merely for brevity. Add a diagram when it clarifies the process or structure.
- In the result, briefly state what was done, how it was checked and remaining limits. Do not repeat the entire work history.
- Deliver completed files as Markdown links with absolute paths, for example [Result](/absolute/path/file).

## Project documentation organization

For a small project, keep CLAUDE.md simple. Do not create a set of documents in advance.

Use CLAUDE.md as a brief entry point to the project. Do not duplicate general Claude Code instructions in it.

Before work, select documents based on the affected functions and dependencies. When the task expands, read the corresponding additional requirements.

Use existing documents. Do not duplicate rules across files or create long chains of references.

When the structure changes, update the relevant links and descriptions. Verify that paths exist.

On first accessing the project's CLAUDE.md within a task, check its size in bytes. Do not measure again while the file is unchanged.

If it exceeds 20,480 bytes, reorganize it before completing the task, without separate approval, targeting an CLAUDE.md size of no more than 15,360 bytes:

1. Compare the requirements with existing documentation.
2. Keep the project's purpose, current working paths, LEARNING.md location, mandatory restrictions, brief project rules and a navigation guide in CLAUDE.md.
3. Move detailed topical requirements into suitable existing documents. Create new documents only when necessary.
4. For each link, state when the document must be read before the corresponding work.
5. Check that requirements and applicability conditions are preserved and links are correct.

Reconciling unique safety conditions is part of this work, not a reason to defer it.

The target size does not permit losing requirements or making mandatory restrictions less prominent. Do not shorten requirements to meet a size target. Do not move mandatory restrictions into documents the agent might skip. Do not change the meaning of rules.

Reorganization may be stopped by an explicit user or project prohibition, unavailable write access, or a specific requirements conflict that cannot be resolved from the available information. In that case, state the obstacle. A small main task is not, by itself, an obstacle.

After reorganization, briefly state the CLAUDE.md size before and after, the documents changed, and the results of checking links and requirement preservation. An explanation is required only if there is an obstacle or the target size cannot be reached safely.

Add new details to topical documents and confirmed lessons to LEARNING.md. Do not return relocated sections to CLAUDE.md unnecessarily.

Splitting is also permitted below the threshold when the file contains detailed requirements for several independent subsystems.

Do not claim token or time savings without measurements.
