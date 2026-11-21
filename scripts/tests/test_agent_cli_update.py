#!/usr/bin/env python3
"""Check update validation and activation using offline, synthetic packages."""
import base64
import fcntl
import hashlib
import io
import json
from pathlib import Path
import runpy
import struct
import subprocess
import tarfile
import tempfile
from unittest.mock import patch

PROJECT = Path(__file__).resolve().parents[2]
ASSETS = PROJECT / "app/src/main/assets"
UPDATER = runpy.run_path(str(ASSETS / "agent-cli-update.py"))
install = UPDATER["install"]
failure = UPDATER["UpdateFailure"]
base = PROJECT / ".verification/agent-cli-update"
base.mkdir(parents=True, exist_ok=True)


def rejects(code, action):
    try:
        action()
    except failure as error:
        assert str(error) == code, error
    else:
        raise AssertionError(f"Expected {code}")


def archive(path, entries):
    with tarfile.open(path, mode="w:gz") as stream:
        for name, data in entries:
            member = tarfile.TarInfo(name)
            member.mode = 0o755
            if data is None:
                member.type = tarfile.SYMTYPE
                member.linkname = "/outside"
                stream.addfile(member)
            else:
                member.size = len(data)
                stream.addfile(member, io.BytesIO(data))


with tempfile.TemporaryDirectory(prefix="offline-", dir=base) as scratch:
    stage = Path(scratch)
    package = stage / "pkg.tgz"
    voice = "package/vendor/aarch64-unknown-linux-musl/codex-resources/voice/bin"
    codex = "package/" + UPDATER["CODEX_BINARY"]
    archive(package, [(codex, b"ELF"), (voice + "/host", b"voice")])
    UPDATER["extract"](package, stage / "safe", 8)
    assert (stage / "safe" / UPDATER["CODEX_BINARY"]).read_bytes() == b"ELF"
    assert not (stage / "safe/vendor/aarch64-unknown-linux-musl/codex-resources/voice").exists()
    rejects("VERIFY", lambda: UPDATER["verify_runtime"](stage / "safe" / UPDATER["CODEX_BINARY"],
                                                      stage, "codex", None, "1.2.3"))
    for index, entries in enumerate([
        [("package/../outside", b"x")],
        [("package/symlink", None)],
        [("package/duplicate", b"x"), ("package/duplicate", b"y")],
        [("package/excess", b"012345678")],
    ]):
        archive(package, entries)
        rejects("PACKAGE", lambda: UPDATER["extract"](package, stage / f"rejected-{index}", 8))

    archive(package, [("package/claude", b"payload")])
    contents = package.read_bytes()
    payload_hash = hashlib.sha512(contents).digest()
    payload_integrity = "sha512-" + base64.b64encode(payload_hash).decode("ascii")
    registry = UPDATER["REGISTRY"]
    parent = "@anthropic-ai/claude-code"
    platform = parent + "-linux-arm64"
    cert_tarball = f"{registry}/{platform}/-/claude-code-linux-arm64-2.1.2.tgz"
    metadata = {
        f"{registry}/{parent}/latest": {"name": parent, "version": "2.1.2",
                                      "optionalDependencies": {platform: "2.1.2"}},
        f"{registry}/{platform}/2.1.2": {"name": platform, "version": "2.1.2", "libc": ["glibc"],
                                      "os": ["linux"], "cpu": ["arm64"],
                                      "dist": {"tarball": cert_tarball, "integrity": payload_integrity,
                                               "unpackedSize": 7}},
    }
    transfer = {"bytes": contents}

    def download(_, url, dest, limit, seconds):
        assert url == cert_tarball or url in metadata
        assert seconds <= 360
        if url == cert_tarball:
            data = transfer["bytes"]
        else:
            data = json.dumps(metadata[url]).encode("utf-8")
        assert len(data) <= limit
        dest.write_bytes(data)

    def verify_candidate(binary, _, agent, __, version):
        assert agent == "claude" and version == "2.1.2"
        assert binary.read_bytes() == b"payload"

    home = stage / "home"
    root = home / ".local/share/termux-jobs/cli-updates/claude"
    old = root / "2.1.1/claude"
    old.parent.mkdir(parents=True)
    old.write_bytes(b"old")
    old.chmod(0o700)
    (root / "current").symlink_to("2.1.1")
    pin = home / ".local/share/termux-jobs/claude-code/2.1.296/claude"
    pin.parent.mkdir(parents=True)
    pin.write_bytes(b"pin")
    pin.chmod(0o700)
    path = "/usr/bin:/bin"
    resolver = ASSETS / "agent-cli-resolver.sh"

    def lookup(ubuntu):
        return Path(subprocess.check_output(
            ["/bin/sh", "-c", '. "$1"; jobs_agent_cli_binary claude', "lookup", resolver],
            env={"HOME": str(home), "PATH": path, "JOBS_AGENT_UBUNTU": ubuntu},
            text=True, timeout=5).strip())

    with patch.dict(install.__globals__, {"download": download, "verify_runtime": verify_candidate,
                                          "probe": lambda *args: "2.1.1"}):
        assert lookup("ubuntu-ai") == old and lookup("ubuntu") == pin
        rejects("VERSION_CHANGED", lambda: install("curl", root, "claude", "2.1.3", home, path, None))
        with (root / "install.lock").open("wb") as busy:
            fcntl.flock(busy, fcntl.LOCK_EX | fcntl.LOCK_NB)
            rejects("BUSY", lambda: install("curl", root, "claude", "2.1.2", home, path, None))
        metadata[f"{registry}/{platform}/2.1.2"]["dist"]["tarball"] = "https://example.invalid/pkg"
        rejects("PACKAGE", lambda: install("curl", root, "claude", "2.1.2", home, path, None))
        metadata[f"{registry}/{platform}/2.1.2"]["dist"]["tarball"] = cert_tarball
        transfer["bytes"] = b"corrupt"
        rejects("PACKAGE", lambda: install("curl", root, "claude", "2.1.2", home, path, None))
        assert lookup("ubuntu-ai") == old and not (root / "2.1.2").exists()
        transfer["bytes"] = contents
        install("curl", root, "claude", "2.1.2", home, path, None)
        assert lookup("ubuntu-ai") == root / "2.1.2/claude"
        assert lookup("ubuntu") == pin and old.read_bytes() == b"old"
        assert not any(root.glob(".stage-*"))

    archive(package, [("README", b"document"), ("antigravity", b"agy binary")])
    payload = package.read_bytes()
    version = "1.5.14"
    agy_tarball = f"{UPDATER['AGY_ARCHIVES']}/{version}-123456/linux-arm/cli_linux_arm64.tar.gz"
    agy_manifest = {"version": version, "url": agy_tarball, "sha512": hashlib.sha512(payload).hexdigest()}
    agy_root = home / ".local/share/termux-jobs/cli-updates/antigravity"
    pin = home / ".local/share/termux-jobs/antigravity-cli/1.5.13/agy"
    pin.parent.mkdir(parents=True)
    pin.write_bytes(b"old agy")
    pin.chmod(0o700)
    transfers = []

    def agy_download(_, url, dest, limit, seconds):
        assert url in (UPDATER["AGY_MANIFEST"], agy_tarball)
        transfers.append(url)
        data = json.dumps(agy_manifest).encode("utf-8") if url == UPDATER["AGY_MANIFEST"] else payload
        assert len(data) <= limit and seconds <= 360
        dest.write_bytes(data)

    def agy_verify(binary, _, agent, __, proposed):
        assert agent == "antigravity" and proposed == version
        assert binary.read_bytes() == b"agy binary"
        assert not (binary.parent / "README").exists()

    with patch.dict(install.__globals__, {"download": agy_download, "verify_runtime": agy_verify,
                                          "probe": lambda *args: "1.5.13"}):
        assert UPDATER["resolved_cli"]("antigravity", home, path) == pin
        assert UPDATER["release"]("curl", stage, "antigravity", None)[0] == version
        assert transfers == [UPDATER["AGY_MANIFEST"]] and not agy_root.exists()
        for wrong in ("https://example.invalid/antigravity.tar.gz", agy_tarball.replace("linux-arm", "linux-amd")):
            agy_manifest["url"] = wrong
            rejects("PACKAGE", lambda: UPDATER["release"]("curl", stage, "antigravity", None))
        agy_manifest["url"] = agy_tarball
        rejects("VERSION_CHANGED", lambda: install("curl", agy_root, "antigravity", "1.5.15", home, path, None))
        install("curl", agy_root, "antigravity", version, home, path, None)
        assert UPDATER["resolved_cli"]("antigravity", home, path) == agy_root / version / "agy"
        assert pin.read_bytes() == b"old agy"

        empty_home = stage / "empty-home"
        empty_home.mkdir()
        empty_root = empty_home / agy_root.relative_to(home)
        assert UPDATER["resolved_cli"]("antigravity", empty_home, path) is None
        install("curl", empty_root, "antigravity", version, empty_home, path, None)
        assert UPDATER["resolved_cli"]("antigravity", empty_home, path) == empty_root / version / "agy"

    # Verify the extracted ELF header, then substitute a shell fixture at the
    # subprocess boundary to exercise the real output handling without running agy.
    binary = stage / "agy-elf"
    header = bytearray(64)
    header[:7] = b"\x7fELF\x02\x01\x01"
    struct.pack_into("<HHI", header, 16, 2, 183, 1)
    binary.write_bytes(header)
    binary.chmod(0o700)
    fake_agy = stage / "fake-agy"
    real_run = subprocess.run

    def run_fake_agy(args, **kwargs):
        assert args[0] == binary
        return real_run(["/bin/sh", fake_agy, *args[1:]], **kwargs)

    help_text = "printf '%s\\n' '--input-format --output-format --conversation --mode stream-json' >&2"
    version_text = f"printf '%s\\n' 'antigravity {version}'"

    def write_agy_probe(version_command, help_command):
        fake_agy.write_text(
            f'case "$1" in\n--version) {version_command};;\n--help) {help_command};;\nesac\n',
            encoding="ascii")

    def verify_agy_probe():
        UPDATER["verify_runtime"](binary, stage, "antigravity", None, version)

    with patch.object(subprocess, "run", side_effect=run_fake_agy):
        write_agy_probe(version_text, help_text)
        verify_agy_probe()
        for invalid_help in ("printf '%s\\n' '--input-format' >&2", help_text + "; exit 1",
                             help_text + "; printf '%65537s' x >&2"):
            write_agy_probe(version_text, invalid_help)
            rejects("VERIFY", verify_agy_probe)
        write_agy_probe(version_text + " >&2", help_text)
        rejects("VERIFY", verify_agy_probe)

    for index, entries in enumerate([
        [("antigravity", None)],
        [("antigravity", b"first"), ("antigravity", b"second")],
        [("../antigravity", b"outside")],
    ]):
        archive(package, entries)
        rejects("PACKAGE", lambda: UPDATER["extract_antigravity"](package, stage / f"agy-rejected-{index}"))

print("PASS updater archive validation, voice exclusion, ELF rejection, stale and busy updates, "
      "digest and origin validation, activation, prior runtime preservation, Antigravity initial installation, "
      "stderr help size and status checks, read-only metadata and Ubuntu scope")
