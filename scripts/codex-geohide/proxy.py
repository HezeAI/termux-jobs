#!/usr/bin/env python3
"""Loopback CONNECT relay. Never terminates TLS, reads auth, or records payloads."""
import argparse
import asyncio
import ipaddress
import json
import re
import signal
import socket
from pathlib import Path

SERVICE = "codex-geohide-connect-v1"
DOMAIN = re.compile(r"(?=.{1,253}\Z)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?\Z")


def public(address):
    ip = ipaddress.ip_address(address)
    return ip.is_global and not ip.is_multicast and not ip.is_unspecified


def authority(value):
    host, sep, port = value.lower().rpartition(":")
    if not sep or port != "443" or not DOMAIN.fullmatch(host):
        raise ValueError("destination")
    return host


def load(path):
    config = json.loads(Path(path).read_text())
    if config["listen"] != "127.0.0.1" or not 1024 <= config["port"] <= 65535:
        raise ValueError("loopback required")
    for host, addresses in config["geohide"].items():
        if not DOMAIN.fullmatch(host) or not addresses or not all(public(x) for x in addresses):
            raise ValueError("invalid routing table")
    return config


class Relay:
    def __init__(self, config, connector=None, resolver=None):
        self.config = config
        self.connector = connector or asyncio.open_connection
        self.resolver = resolver
        self.active = 0
        self.serial = 0

    async def addresses(self, host):
        if host in self.config["geohide"]:
            addresses = self.config["geohide"][host]
            # Retry another checked GeoHide IP on a TCP failure; never fall back to direct.
            n = self.serial % len(addresses)
            self.serial += 1
            return addresses[n:] + addresses[:n], "geohide"
        resolver = self.resolver or asyncio.get_running_loop().getaddrinfo
        records = await asyncio.wait_for(resolver(host, 443, type=socket.SOCK_STREAM), 8)
        addresses = list(dict.fromkeys(x[4][0] for x in records))
        if not addresses or not all(public(x) for x in addresses):
            raise ValueError("nonpublic destination")
        return addresses[:8], "dns"

    async def connect(self, host):
        addresses, route = await self.addresses(host)
        for address in addresses:
            try:
                reader, writer = await asyncio.wait_for(
                    self.connector(address, 443), self.config["connect_timeout"])
                return reader, writer, route
            except (OSError, TimeoutError):
                continue
        raise ConnectionError("upstream unavailable")

    async def pipe(self, reader, writer):
        while True:
            data = await asyncio.wait_for(reader.read(65536), self.config["idle_timeout"])
            if not data:
                if writer.can_write_eof():
                    writer.write_eof()
                    await asyncio.wait_for(writer.drain(), 15)
                return
            writer.write(data)
            await asyncio.wait_for(writer.drain(), 15)

    async def handle(self, reader, writer):
        upstream = None
        tasks = []
        counted = False
        established = False
        host = "-"
        try:
            if self.active >= self.config["max_connections"]:
                writer.write(b"HTTP/1.1 503 Busy\r\nConnection: close\r\n\r\n")
                return
            self.active += 1
            counted = True
            raw = await asyncio.wait_for(reader.readuntil(b"\r\n\r\n"), 8)
            if len(raw) > 8192:
                raise ValueError("header limit")
            lines = raw.decode("ascii").split("\r\n")
            method, target, version = lines[0].split(" ")
            if version not in ("HTTP/1.0", "HTTP/1.1"):
                raise ValueError("version")
            # Only header names are inspected. No header values are logged or forwarded.
            names = []
            for line in lines[1:-2]:
                name, sep, _ = line.partition(":")
                if not sep or not re.fullmatch(r"[A-Za-z0-9-]+", name):
                    raise ValueError("header")
                names.append(name.lower())
            if "transfer-encoding" in names or "content-length" in names:
                raise ValueError("body not supported")
            if method == "GET" and target == "/health":
                body = json.dumps({"service": SERVICE}).encode()
                writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\nContent-Length: " + str(len(body)).encode() + b"\r\n\r\n" + body)
                return
            if method != "CONNECT":
                writer.write(b"HTTP/1.1 405 CONNECT required\r\nConnection: close\r\n\r\n")
                return
            host = authority(target)
            other_reader, upstream, route = await self.connect(host)
            # Only domain/route: no URLs, query strings, request bodies, headers or tokens.
            print(json.dumps({"event": "connect", "host": host, "route": route}), flush=True)
            writer.write(b"HTTP/1.1 200 Connection established\r\n\r\n")
            await asyncio.wait_for(writer.drain(), 15)
            established = True
            tasks = [asyncio.create_task(self.pipe(reader, upstream)),
                     asyncio.create_task(self.pipe(other_reader, writer))]
            await asyncio.gather(*tasks)
        except (ValueError, UnicodeError, asyncio.IncompleteReadError, asyncio.LimitOverrunError):
            if not established:
                writer.write(b"HTTP/1.1 400 Invalid CONNECT\r\nConnection: close\r\n\r\n")
        except (OSError, TimeoutError):
            if not established:
                writer.write(b"HTTP/1.1 502 Upstream unavailable\r\nConnection: close\r\n\r\n")
            print(json.dumps({"event": "connection_failed", "host": host}), flush=True)
        finally:
            for task in tasks:
                if not task.done():
                    task.cancel()
            if tasks:
                await asyncio.gather(*tasks, return_exceptions=True)
            for stream in (upstream, writer):
                if stream:
                    try:
                        await asyncio.wait_for(stream.drain(), 2)
                    except (OSError, TimeoutError):
                        pass
                    stream.close()
            if counted:
                self.active -= 1


async def main(config):
    relay = Relay(config)
    server = await asyncio.start_server(relay.handle, config["listen"], config["port"], limit=8192)
    done = asyncio.Event()
    for sig in (signal.SIGINT, signal.SIGTERM):
        asyncio.get_running_loop().add_signal_handler(sig, done.set)
    print(json.dumps({"event": "listening", "address": config["listen"], "port": config["port"]}), flush=True)
    async with server:
        await done.wait()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("config")
    args = parser.parse_args()
    asyncio.run(main(load(args.config)))
