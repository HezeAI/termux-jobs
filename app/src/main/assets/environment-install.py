#!/usr/bin/env python3
"""Own one installer process group; cancellation never targets another session."""
import errno
import os
from pathlib import Path
import pty
import re
import select
import signal
import subprocess
import sys
import time


def supervise(script, alias, cancel):
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,63}', alias):
        raise ValueError('INVALID_ALIAS')
    script, cancel = Path(script), Path(cancel)
    if script.name != 'runtime.sh' or cancel.parent != script.parent or not re.fullmatch(r'cancel-[0-9a-f-]{36}', cancel.name):
        raise ValueError('INVALID_CONTROL_PATH')
    if cancel.exists():
        print('ERROR:INSTALL_CANCELLED', flush=True)
        return 130
    requested = False

    def request(_signum, _frame):
        nonlocal requested
        requested = True
    signal.signal(signal.SIGTERM, request)
    signal.signal(signal.SIGINT, request)
    master, slave = pty.openpty()
    process = None
    group_pinned = False
    try:
        process = subprocess.Popen([os.environ['PREFIX']+'/bin/bash', '--noprofile', '--norc', str(script), 'install', alias],
                                   stdin=subprocess.DEVNULL, stdout=slave, stderr=slave, start_new_session=True)
        os.close(slave)
        slave = -1
        stopped = False
        deadline = None
        stop_stage = 0
        stream_open = True
        ended_at = None
        while True:
            # Do not reap the group leader during cancellation: its reserved PID
            # keeps the group identity pinned until every escalation is sent.
            status = None if group_pinned else process.poll()
            if status is None and (requested or cancel.exists()):
                # A live, unreaped child pins its PID. start_new_session owns this group.
                if not stopped:
                    try:
                        os.killpg(process.pid, signal.SIGINT)
                    except ProcessLookupError:
                        pass
                    stopped = True
                    group_pinned = True
                    stop_stage = 1
                    # Let Python installers handle KeyboardInterrupt and clean their
                    # own partial state. Bound this grace: shell descendants can
                    # ignore SIGINT and must not keep running after cancellation.
                    deadline = time.monotonic()+1
                elif deadline is not None and time.monotonic() >= deadline:
                    try:
                        os.killpg(process.pid, signal.SIGTERM if stop_stage == 1 else signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                    deadline = time.monotonic()+3 if stop_stage == 1 else None
                    if stop_stage == 2:
                        # KILL was sent while the leader was still unreaped.
                        group_pinned = False
                        status = process.poll()
                    stop_stage = 2
            if stream_open and select.select([master], [], [], .1)[0]:
                try:
                    data = os.read(master, 65536)
                except OSError as error:
                    if error.errno != errno.EIO:
                        raise
                    data = b''
                if data:
                    sys.stdout.buffer.write(data)
                    sys.stdout.buffer.flush()
                else:
                    stream_open = False
            elif not stream_open and status is None:
                time.sleep(.1)
            if status is not None:
                if ended_at is None:
                    ended_at = time.monotonic()
                if not stream_open or time.monotonic()-ended_at >= .5:
                    break
        if stopped:
            print('\nERROR:INSTALL_CANCELLED', flush=True)
            return 130
        return process.wait()
    finally:
        if slave >= 0:
            os.close(slave)
        os.close(master)
        # Unexpected errors during the grace must also clear the owned group,
        # before any wait/poll can release its leader's identity.
        if process is not None and group_pinned:
            try:
                os.killpg(process.pid, signal.SIGTERM)
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()
        # Unexpected supervisor errors must not abandon an installer.
        if process is not None and process.poll() is None:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=3)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                process.wait()


if __name__ == '__main__':
    try:
        if len(sys.argv) != 4:
            raise ValueError('INVALID_ARGUMENTS')
        sys.exit(supervise(*sys.argv[1:]))
    except Exception:
        print('ERROR:INSTALL_SUPERVISOR_FAILED', flush=True)
        sys.exit(1)
