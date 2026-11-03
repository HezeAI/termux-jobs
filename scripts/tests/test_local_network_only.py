#!/usr/bin/env python3
"""Test-only kernel guard. Uses artificial addresses/data, never account files."""
import ctypes
import errno
import json
import os
from pathlib import Path
import platform
import socket
import subprocess
import sys

if '--grandchild' in sys.argv:
    probe = socket.socket()
    assert probe.connect_ex(('203.0.113.2', 443)) == errno.EPERM
    raise SystemExit(0)

if '--child' in sys.argv:
    foreign = int(sys.argv[-1])
    result = {}
    server = socket.socket()
    server.bind(('127.0.0.1', 0))
    server.listen()
    client = socket.socket()
    client.connect(server.getsockname())
    accepted, _ = server.accept()
    client.sendall(b'SYNTHETIC_LOCAL_ONLY')
    assert accepted.recv(64) == b'SYNTHETIC_LOCAL_ONLY'
    result['owned_listener'] = 'allowed'
    for value in (accepted, client, server):
        value.close()
    for name, host, port in [('external', '203.0.113.1', 443), ('foreign_localhost', '127.0.0.1', foreign)]:
        probe = socket.socket()
        assert probe.connect_ex((host, port)) == errno.EPERM, name
        probe.close()
        result[name] = 'EPERM'
    for family, kind, name in [(socket.AF_INET, socket.SOCK_DGRAM, 'udp'), (socket.AF_PACKET, socket.SOCK_RAW, 'packet')]:
        try:
            socket.socket(family, kind)
            raise AssertionError(name + ' escaped')
        except OSError as error:
            assert error.errno == errno.EPERM
        result[name] = 'EPERM'
    try:
        socket.socket(socket.AF_INET, socket.SOCK_STREAM, 132)  # SCTP cannot add unmediated connect paths.
        raise AssertionError('non-TCP stream escaped')
    except OSError as error:
        assert error.errno == errno.EPERM
    result['non_tcp_stream'] = 'EPERM'
    probe = socket.socket()
    try:
        probe.setsockopt(socket.IPPROTO_TCP, 30, 1)  # TCP_FASTOPEN_CONNECT
        raise AssertionError('deferred connect escaped')
    except OSError as error:
        assert error.errno == errno.EPERM
    probe.close()
    result['deferred_connect'] = 'EPERM'
    try:
        probe = socket.socket(socket.AF_INET6)
    except OSError as error:
        assert error.errno in (errno.EAFNOSUPPORT, errno.EPROTONOSUPPORT)
        result['ipv6'] = 'kernel unavailable; not passed'
    else:
        assert probe.connect_ex(('2001:db8::1', 443)) == errno.EPERM
        probe.close()
        result['ipv6'] = 'EPERM'
    # Bypass Python's socket API with native syscall(2); the filter still applies.
    library = ctypes.CDLL(None, use_errno=True)
    socket_nr, connect_nr = (198, 203) if platform.machine() == 'aarch64' else (41, 42)
    fd = library.syscall(socket_nr, socket.AF_INET, socket.SOCK_STREAM, 0)
    assert fd >= 0
    address = ctypes.create_string_buffer(bytes([2, 0]) + socket.htons(443).to_bytes(2, sys.byteorder) + socket.inet_aton('203.0.113.3') + b'\0' * 8)
    assert library.syscall(connect_nr, fd, ctypes.byref(address), 16) == -1
    assert ctypes.get_errno() == errno.EPERM
    os.close(fd)
    fast_fd = library.syscall(socket_nr, socket.AF_INET, socket.SOCK_STREAM, 0)
    assert fast_fd >= 0
    payload = ctypes.create_string_buffer(b'SYNTHETIC')
    sendto_nr, sendmsg_nr, sendmmsg_nr = (206, 211, 269) if platform.machine() == 'aarch64' else (44, 46, 307)
    assert library.syscall(sendto_nr, fast_fd, ctypes.byref(payload), 1, 0x20000000, ctypes.byref(address), 16) == -1
    assert ctypes.get_errno() == errno.EPERM
    assert library.syscall(sendmsg_nr, fast_fd, None, 0x20000000) == -1
    assert ctypes.get_errno() == errno.EPERM
    assert library.syscall(sendmmsg_nr, fast_fd, None, 0, 0x20000000) == -1
    assert ctypes.get_errno() == errno.EPERM
    os.close(fast_fd)
    result['send_fastopen'] = 'EPERM'
    result['raw_syscall'] = 'EPERM'
    subprocess.run([sys.executable, __file__, '--grandchild'], check=True, timeout=5)
    result['exec_inheritance'] = 'blocked'
    print(json.dumps(result))
    raise SystemExit(0)

if len(sys.argv) != 3:
    raise SystemExit('usage: test_local_network_only.py GUARD OUTPUT_DIRECTORY')
guard = Path(sys.argv[1]).resolve()
output = Path(sys.argv[2]).resolve()
output.mkdir(parents=True, exist_ok=True)
foreign = socket.socket()
foreign.bind(('127.0.0.1', 0))
foreign.listen()
foreign.settimeout(0.1)
try:
    env = {'PATH': os.environ['PATH'], 'HOME': str(output)}
    complete = subprocess.run([str(guard), str(output / 'network-events.jsonl'), '--', sys.executable, str(Path(__file__).resolve()), '--child', str(foreign.getsockname()[1])], env=env, capture_output=True, text=True, timeout=15)
    (output / 'selftest.stdout').write_text(complete.stdout)
    (output / 'selftest.stderr').write_text(complete.stderr)
    assert complete.returncode == 0, complete.stderr
    try:
        unexpected, _ = foreign.accept()
        unexpected.close()
        raise AssertionError('Guard touched an unregistered host listener')
    except TimeoutError:
        pass
    result = json.loads(complete.stdout)
    (output / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result))
finally:
    foreign.close()
