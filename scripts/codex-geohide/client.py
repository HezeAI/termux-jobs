#!/usr/bin/env python3
"""Own one reconnecting SSH forward, scope proxy environment to a new Codex."""
import fcntl
import hmac
import json
import os
from pathlib import Path
import selectors
import secrets
import shutil
import signal
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / ".tools/codex-geohide"
STATE = BASE / "state"
CONTROL = STATE / "control.sock"
KEY = STATE / "control.key"
SERVICE = "codex-geohide-connect-v1"


def config():
    return json.loads((BASE / "client.json").read_text())


def prepare():
    os.umask(0o077)
    for folder in (BASE, STATE):
        if folder.is_symlink():
            raise RuntimeError("Private tunnel directory is a symlink")
        folder.mkdir(mode=0o700, parents=True, exist_ok=True)
        folder.chmod(0o700)
    if KEY.is_symlink():
        raise RuntimeError("Control key is a symlink")
    try:
        fd = os.open(KEY, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        pass
    else:
        with os.fdopen(fd, "w") as output:
            output.write(secrets.token_hex(32))


def control_key():
    if KEY.is_symlink() or KEY.stat().st_mode & 0o077:
        raise ValueError("Unsafe control key permissions")
    value = KEY.read_text()
    if len(value) != 64 or any(x not in "0123456789abcdef" for x in value):
        raise ValueError("Invalid control key")
    return value


def command(op):
    try:
        c = config()
        tcp = "control_port" in c
        with socket.socket(socket.AF_INET if tcp else socket.AF_UNIX) as sock:
            sock.settimeout(2)
            sock.connect(("127.0.0.1", c["control_port"]) if tcp else str(CONTROL))
            request = json.dumps({"op": op, "token": control_key()}).encode() if tcp else op.encode()
            sock.sendall(request + b"\n")
            data = sock.recv(4096)
        return json.loads(data)
    except (OSError, ValueError):
        return {"phase": "stopped", "ready": False}


def healthy(c):
    try:
        # Explicitly disable inherited proxy settings for this loopback-only probe.
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        with opener.open("http://127.0.0.1:%d/health" % c["local_port"], timeout=2) as response:
            return json.loads(response.read(512)).get("service") == SERVICE
    except (OSError, ValueError):
        return False


def ssh_command(c):
    return [c["ssh"], "-F", "/dev/null", "-N", "-T",
            "-i", c["identity"], "-o", "UserKnownHostsFile=" + c["known_hosts"],
            "-o", "GlobalKnownHostsFile=/dev/null", "-o", "StrictHostKeyChecking=yes",
            "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes", "-o", "ExitOnForwardFailure=yes",
            "-o", "ConnectTimeout=12", "-o", "ServerAliveInterval=10",
            "-o", "ServerAliveCountMax=3", "-o", "TCPKeepAlive=yes", "-o", "LogLevel=ERROR",
            "-o", "ControlMaster=no", "-o", "ControlPath=none", "-o", "ForwardAgent=no",
            "-o", "ClearAllForwardings=no", "-L",
            "127.0.0.1:%d:127.0.0.1:%d" % (c["local_port"], c["remote_port"]), c["server"]]


def stop_owned(child):
    # A live Popen handle from this manager only; never discover or kill other PIDs.
    if child is not None and child.poll() is None:
        child.terminate()
        try:
            child.wait(timeout=3)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait(timeout=3)


def supervise(c):
    prepare()
    with (STATE / "manager.lock").open("a+") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return
        tcp = "control_port" in c
        if not tcp:
            CONTROL.unlink(missing_ok=True)
        control_secret = control_key() if tcp else None
        with socket.socket(socket.AF_INET if tcp else socket.AF_UNIX) as listener:
            if tcp:
                listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            listener.bind(("127.0.0.1", c["control_port"]) if tcp else str(CONTROL))
            if not tcp:
                CONTROL.chmod(0o600)
            listener.listen(4)
            listener.setblocking(False)
            selector = selectors.DefaultSelector()
            selector.register(listener, selectors.EVENT_READ)
            running = True
            child = None
            phase, ready, generation, failures = "starting", False, 0, 0
            next_start = next_health = 0
            last_exit = None
            def ending(*_):
                nonlocal running
                running = False
            for sig in (signal.SIGTERM, signal.SIGINT):
                signal.signal(sig, ending)
            logpath = STATE / "tunnel.log"
            if logpath.is_symlink():
                raise RuntimeError("Tunnel log is a symlink")
            if logpath.exists() and logpath.stat().st_size > 262144:
                logpath.write_text("")
            with logpath.open("a", buffering=1) as log:
                try:
                    while running:
                        now = time.monotonic()
                        if child is not None and child.poll() is not None:
                            last_exit = child.returncode
                            child = None
                            ready, phase = False, "reconnecting"
                            failures += 1
                            next_start = now + min(30, 2 ** min(failures, 5))
                            print("SSH ended (%d); reconnecting" % last_exit, file=log)
                        if child is None and now >= next_start:
                            child = subprocess.Popen(ssh_command(c), stdin=subprocess.DEVNULL,
                                stdout=subprocess.DEVNULL, stderr=log)
                            generation += 1
                            phase, ready, next_health = "connecting", False, now + 1
                        if child is not None and now >= next_health:
                            ready = healthy(c)
                            phase = "ready" if ready else "connecting"
                            if ready:
                                failures = 0
                            next_health = now + 3
                        for key, _ in selector.select(0.5):
                            connection, _ = key.fileobj.accept()
                            with connection:
                                connection.settimeout(1)
                                try:
                                    raw = connection.recv(4096).strip().decode("ascii")
                                    if tcp:
                                        request = json.loads(raw)
                                        token = request.get("token", "")
                                        if not isinstance(token, str) or not hmac.compare_digest(token, control_secret):
                                            connection.sendall(b'{"error":"unauthorized"}\n')
                                            continue
                                        op = request.get("op", "")
                                    else:
                                        op = raw
                                    reply = {"phase": phase, "ready": ready, "generation": generation,
                                             "ssh_pid": child.pid if child is not None else None,
                                             "last_exit": last_exit}
                                    if op == "stop":
                                        running = False
                                        reply.update(phase="stopping", ready=False)
                                    elif op == "reconnect":
                                        stop_owned(child)
                                        child = None
                                        ready, phase, next_start = False, "reconnecting", time.monotonic() + 1
                                        reply.update(phase=phase, ready=False)
                                    elif op != "status":
                                        reply = {"error": "unknown command"}
                                    connection.sendall(json.dumps(reply).encode() + b"\n")
                                except (OSError, ValueError, AttributeError):
                                    pass
                        if logpath.stat().st_size > 1048576:
                            log.seek(0)
                            log.truncate()
                finally:
                    stop_owned(child)
                    selector.close()
                    if not tcp:
                        CONTROL.unlink(missing_ok=True)


def start(c):
    prepare()
    for key in ("identity", "known_hosts", "ssh"):
        if not Path(c[key]).is_file():
            raise RuntimeError("Missing " + key)
    deadline = time.monotonic() + 45
    last_spawn = 0
    announced = False
    while time.monotonic() < deadline:
        state = command("status")
        if state.get("ready") and healthy(c):
            return state
        if not announced:
            print("Подключение к Selectel и проверка прокси (до 45 секунд)…", file=sys.stderr)
            announced = True
        # Also handles an immediate start following stop, without reusing a dying manager.
        if state.get("phase") == "stopped" and time.monotonic() - last_spawn > 2:
            with (STATE / "manager.log").open("ab") as output:
                subprocess.Popen([sys.executable, "-B", str(Path(__file__).resolve()), "_daemon"],
                    stdin=subprocess.DEVNULL, stdout=output, stderr=output,
                    start_new_session=True, close_fds=True)
            last_spawn = time.monotonic()
        time.sleep(0.5)
    raise RuntimeError("SSH/relay unavailable; Codex was not launched. Check status and " + str(STATE / "tunnel.log"))


def proxy_environment(c):
    env = dict(os.environ)
    for key in list(env):
        if key.lower() in ("http_proxy", "https_proxy", "all_proxy", "ws_proxy", "wss_proxy", "no_proxy"):
            del env[key]
    url = "http://127.0.0.1:%d" % c["local_port"]
    for name in ("HTTP_PROXY", "HTTPS_PROXY", "WS_PROXY", "WSS_PROXY", "ALL_PROXY"):
        env[name] = env[name.lower()] = url
    env["NO_PROXY"] = env["no_proxy"] = "localhost,127.0.0.1,::1"
    return env


def main():
    c = config()
    args = sys.argv[1:]
    op = args[0] if args else "run"
    if op == "_daemon":
        supervise(c)
    elif op in ("status", "stop", "reconnect"):
        print(json.dumps(command(op), ensure_ascii=False))
    elif op == "start":
        print(json.dumps(start(c), ensure_ascii=False))
    elif op == "command":
        if len(args)<2:
            raise RuntimeError("Agent command is missing")
        start(c)
        print("Агент через Selectel → GeoHide; TLS проверяется.", file=sys.stderr)
        os.execvpe(args[1],args[1:],proxy_environment(c))
    else:
        if op == "run":
            args = args[1:]
        start(c)
        # A shared app-server can retain its old environment. Always use a separate process.
        executable = shutil.which(c["codex"])
        if not executable:
            raise RuntimeError("Codex not found in this Ubuntu")
        print("Codex через Selectel → GeoHide; отдельный процесс, TLS проверяется.", file=sys.stderr)
        os.execvpe(executable, [executable, "--no-daemon", *args], proxy_environment(c))


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, ValueError, KeyError) as error:
        print("Ошибка туннеля: " + str(error), file=sys.stderr)
        raise SystemExit(1)
