# Project instructions

## Purpose

- Project: [name].
- Objective: [what the project does].
- Working directory: [path].
- Main sources: [path].
- Deliverables: [path].
- Project memory: LEARNING.md.

Read LEARNING.md before working if it exists.
Follow the environment's general instructions. This file adds requirements specific to the project.

## Documentation

Read the documents relevant to the task:

- README.md — project structure and how to run it.
- [verification document] — before changing the relevant features.
- RELEASING.md — before preparing or publishing a release.

List only existing documents. Create additional documentation only when necessary.

## Starting work

1. Check the current branch and uncommitted changes.
2. Review the source and documentation for the affected feature.
3. Identify the applicable checks.
4. Preserve the user's existing work.

Do not assume existing changes in the working tree are yours.
Keep unrelated changes out of your commits.

## Local Git workflow

Local branches and commits are allowed within the assigned task.
These actions do not grant permission to push or publish.

- Use the existing repository.
- If Git is not configured, offer to set it up.
- Continue on a suitable working branch.
- Use a separate branch for independent or risky work.
- Before creating a branch, check what is already in the working tree. A new branch alone does not isolate those changes.
- Commit each logically complete change separately.
- Before committing, review the diff and the staged files.
- Use a commit message that describes the specific change.
- Keep fixes, unrelated refactoring, and formatting separate.
- Do not rewrite existing history without separate permission.

Keep secrets, credentials, private keys, working logs, caches, and temporary files out of Git.

Add built binaries only according to the project's established procedure. The .gitignore file does not remove data already committed.

## Pushing and publishing

Pushing, merging into a shared branch, creating a release tag, deploying, and publishing are separate actions.

Proceed only with user permission, including permission already granted for the current task.
Do not ask for the same permission again without a new reason.

Before pushing:
1. Check the repository URL and target branch.
2. Check which commits will be sent.
3. Run the required checks.
4. After pushing, verify the remote branch.

Do not force-push or move published tags without separate explicit permission.

Pushing source code does not publish the built product.

## Recovery and rollback

Before a risky change, record the starting branch, commit, and existing uncommitted changes.

- Prefer git revert to undo a published change.
- Do not use reset --hard, git clean, or restore files over someone else's work without the applicable permission.
- Account for source, built files, and user data separately.
- Rolling back the code does not roll back the database or settings.
- Prepare and verify a recovery method before an irreversible migration.

Local Git does not replace a backup.
Verify that data exists off-device before claiming it is backed up there.

## Changes and checks

Preserve compatibility unless the task requires changing it.
Leave unrelated parts of the project alone.

Verification commands:
- [verification command].
- [build command, if applicable].

Check the affected features and their dependencies.
For operations that change state, cover failure, cancellation, and retry.

Distinguish results:
- Checked anew.
- Reused after confirming that the inputs match.
- Not checked.

Do not present a test fixture as verification on a real device or service.
If a check is unavailable, state why and identify the remaining step.

## Deliverables

Before handing over a deliverable, verify that it matches the stated source and version.

Record when applicable:
- The source commit.
- The deliverable version.
- The build conditions.
- The file checksum.

If the build included uncommitted changes, disclose them.

## Finishing the task

Briefly report:
1. What changed.
2. What was checked and what limitations remain.
3. Where the result is located.
4. The branch and commits.
5. Whether anything was pushed, installed, or published.

Assess whether there is a confirmed lesson to add to LEARNING.md.
Keep secrets, unconfirmed assumptions, and full activity logs out of that file.
