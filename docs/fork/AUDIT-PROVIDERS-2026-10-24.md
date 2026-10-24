# Locale and legacy provider audit — 2026-10-24

Private **2.0.15-test / code1344** follows the settings audit and retains earlier
private fixes. Public stable remains v2.0.2/code1331. Package com.termux, signer,
schema8, ARM64, minSDK26/target28/compile35 and embedded-X11 minimum API30 are
unchanged. Evidence: `.verification/provider-audit-20261024/`.

## Findings and changes

The SAF TermuxDocumentsProvider accepted arbitrary existing absolute document
IDs, used string prefixes for child/search boundaries, and failed open after
canonicalization errors. Creation used an unchecked parent/name; root deletion
was offered. Listing assumed listFiles never returned null. Search followed
cycles without a visited set or work limit. TermuxOpenReceiver's legacy content
provider also accepted prefix siblings; metadata bypassed its open-file path
check and file size was narrowed to int. Default-locale lowercasing broke MIME
lookup/search for uppercase I under Turkish locale. Manually concatenated share
URIs misinterpreted names containing #, ? or percent signs; invalid incoming
intent actions logged a fallback without applying it.

Three existing sources change: TermuxDocumentsProvider, TermuxOpenReceiver and
TermuxService. One shared production helper, ProviderFiles, is added. The service
change is only Locale.ROOT for its two lock tags; acquisition/release is unchanged.
No runtime asset, native library, agent bridge, environment installer, process
shutdown path, workspace file browser or settings store changes in this stage.

- Resolve absolute, NUL-free paths canonically and compare path components.
  SAF exposes HOME; the legacy file provider keeps its Termux-files/shared-storage
  roots. Resolution failures reject access. Child checks exclude the parent
  itself and require a real directory; invalid/unreadable listings fail cleanly.
- Keep ordinary absolute document IDs and original alias display names/MIME.
  Internal links remain usable; external/dangling links are not exposed by SAF.
  Search tracks visited directories, retains filename aliases, stops at 50 results
  or 10,000 candidate visits, bounds its pending queue and normalizes both sides
  with Locale.ROOT. Per-directory listFiles still obtains a directory snapshot;
  this is not a whole-filesystem memory or latency guarantee.
- Open without O_CREAT/O_TRUNC, with O_NOFOLLOW/O_NONBLOCK/CLOEXEC. Duplicate the
  owned descriptor, close the original, validate the kernel descriptor path and
  require a regular file. Check cancellation before destructive truncation.
  Android mount aliases are accepted only when an ancestor has the same directory
  device/inode identity as an allowed root; an unavailable optional storage root
  does not veto a positively identified private root. Missing/special files are
  rejected. Use the Linux CLOEXEC constant without an API27 Java-field dependency.
- Pin a checked parent directory descriptor for create/delete and address only its
  child basename through /proc/self/fd. Reject path-like display names, preserve
  existing/dangling-link names with exclusive creation and bounded suffix retries,
  forbid root deletion, and unlink internal symlinks without deleting their targets.
- Preserve MANAGE_DOCUMENTS/URI grants, RUN_COMMAND permission and the existing
  allow-external-apps check before opening the legacy file provider. The four
  protected property-file paths retain read-only downgrade; descriptor inode
  comparison additionally covers hard-link aliases before any truncation.
- Build share URIs with Uri.Builder, apply the advertised VIEW fallback, keep
  read-only outgoing grants, and return long file sizes. Unknown-extension fallback
  remains application/octet-stream for shares/SAF and null for legacy getType.

## Tests, review and limits

Three safe old-code control cases all failed as expected: querying a file outside
HOME, prefix-sibling child classification and Turkish-locale MIME. Only unused
root/policy accessor seams were added to let the fixtures compile; their old
security behavior was unchanged. The negative query case stops at its first
failed assertion and does not execute an old-code destructive operation.

Final **42 JVM tests in five classes passed**, no failures/errors/skips:
15 DocumentsProvider, nine OpenReceiver, seven existing incoming-file receiver,
four language and seven export checks. Cases cover invalid IDs/names/URIs,
prefix/traversal escapes, internal/external/dangling links, cycles, stable IDs and
alias metadata, search case folding, root deletion, collision preservation,
creation, cancellation before/after open, descriptor closure, symlink and ancestor
substitution before truncation, mount aliases with unavailable optional roots,
property hard links, policy denial, 3-GiB sparse-file size and encoded share names.

The descriptor tests use an explicit Os/ParcelFileDescriptor model with real
private files/channels and /proc paths. They translate Robolectric's synthetic
file-type bits, give its otherwise-zero NOFOLLOW/CLOEXEC/NONBLOCK flags distinct
nonzero values and populate its empty MIME table. They do not simulate Binder,
Android SELinux/FUSE or a physical device. Early fixture failures exposed these
model gaps; another failure exposed and fixed a null-ID dereference. An initial
compile used non-public O_DIRECTORY; final code checks directory type with fstat.
Final lint caught the API27 CLOEXEC field reference, which was removed before the
final source rerun/build. This evidence is not a complete adversarial-kernel test.

Executed release lint: **0 errors / 118 warnings**, down from 124; all six
DefaultLocale warnings are removed. AndroidX custom-registry Vendor loading and
Kotlin metadata incompatibilities remain, so full clean lint is not claimed.
Localization checks pass for 584 workspace strings and app/shared key/format parity.
The full 1172-JVM/560-Python/31-Node results remain specific to code1342, not this
artifact. Source/stage hashes restrict the production delta to the three sources,
one helper and version metadata; permission/cache/network guards are reviewed.

Descriptor checks precede writes and owned handles close on error/cancellation.
No numeric PID/group shutdown or process lifecycle mechanism is touched. The
providers do not isolate a trusted shell or root process concurrently renaming
already-authorized ancestors after validation, nor provide a filesystem snapshot.
Android physical URI grants, /proc/self/fd behavior, mount alias identities and
shared-storage permissions still need phone verification. No claim is made that
all manufacturer storage implementations or external editor apps are verified.

## Artifact and compatibility

Verified APK: `build/apk/Termux-Jobs-v2.0.15-test-arm64.apk`, **45,847,810 bytes**. SHA-256:
`44aa5a1b01d94edb3be0d6e01a17dc9901311783e793ce494dac4425aa2fc2f8`.
ZIP integrity, com.termux/code1344/name/ARM64/non-debuggable identity, signing
continuity/increased code, runtime class references, tested-source/staged-source
matching and bounded secret-pattern checks passed. Native libraries and runtime
assets match1343. Required missing runtime types: 0; 31 optional types remain reviewed.
No private key/auth file or configured private server address was included by the
artifact checks. This bounded scan is not a universal secret-detection guarantee.

Valid data needs no reset, account migration or environment replacement. Access
is intentionally tighter: saved SAF IDs that resolve outside HOME no longer work;
select those locations through Android storage instead. The legacy sharing
provider opens existing regular files only; clients cannot use it to create a
missing file. SAF creation remains supported through createDocument. Generic
workspace file browsing, project paths and shell access retain their own policies.
No stable release is published by this private audit. A later release must exceed
1344, include update.json from its final APK, and explain these restrictions in
both localized highlights and English public release notes.

Minimum phone check after a chosen update: use Android's Termux document source
to create/open/edit a disposable HOME file, then use termux-open on an ordinary
shared-storage image with a non-ASCII/# filename. Confirm both open and content
survives closing/reopening. An intentional outside-HOME SAF symlink should be
unavailable. These are device checks, not results already obtained here.

The remaining main source audit task is lint-toolchain alignment and warning
triage. Existing remote descendant/durable-job and SDK/application-ID migration
limits are separate outstanding design work.
