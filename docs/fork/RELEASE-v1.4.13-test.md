# v1.4.13-test / code1119

Short, unambiguous latest-file requests now have an application-coordinated discovery
path, shared by all local models. Examples: «посмотри последний тхт в довнолоад»,
«найди последний APK в загрузках», «read the latest markdown file in documents».
The app probes known folders in the selected environment, selects an actually
available Download/Downloads (or Documents/Pictures/Music/Movies) path, lists only
the requested extension or image/video group and sorts by modification time before
limiting to the newest file. An open/read request for supported text extensions
then calls read on that exact returned path. Find-only requests do not auto-read;
binary files are never auto-read as text or installed/executed.

Explicit paths/filenames, questions, negations and compound tasks remain for the
model; this recognizer is not universal natural-language understanding. Named-file
ambiguity retains the existing file-choice card. Only the selected directory is
listed, not all descendants/storage. If no alias is reported available, the
canonical candidate is checked normally, so access/scope/not-found errors remain
visible rather than substituting the project or storage root. Other folders with
similar names are not claimed absent. Directory details now show the checked path.

Each generated tool goes through the existing access, canonical-path and selected
environment checks. Chat-only mode does not trigger discovery. There is no new
permission, package installation, network lookup, model training or vision support.
External Codex/OpenCode/Claude/Gemini behavior is unchanged.

Same com.termux package and signing identity, increased versionCode1119. No data
schema change, history deletion or required reset. Local test delivery only; no
GitHub publication. Device checks are still needed for real models, mounts and UI.
