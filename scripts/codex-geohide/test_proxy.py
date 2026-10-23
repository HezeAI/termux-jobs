import asyncio
import contextlib
import importlib.util
import io
import json
import os
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent
def module(name):
    spec = importlib.util.spec_from_file_location(name, HERE / (name + '.py'))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value
p, c = module('proxy'), module('client')
CONFIG = {'listen':'127.0.0.1','port':18747,'geohide':{'api.openai.com':['193.233.112.67','193.233.112.68']},'connect_timeout':.1,'idle_timeout':2,'max_connections':8}

class Parsing(unittest.TestCase):
    def test_connect_authority_requires_hostname_and_https(self):
        self.assertEqual('api.openai.com',p.authority('API.OPENAI.COM:443'))
        for bad in ['api.openai.com:80','localhost:443','127.0.0.1:443','[::1]:443','api.openai.com@evil.example:443','evil.example/path:443','api.openai.com:443\r\n']:
            with self.assertRaises(ValueError):p.authority(bad)
    def test_private_and_metadata_addresses_are_blocked(self):
        for address in ['127.0.0.1','10.1.1.1','169.254.169.254','192.168.0.1','::1','fc00::1','224.0.0.1']:
            self.assertFalse(p.public(address))
        self.assertTrue(p.public('193.233.112.67'))
    def test_nonloopback_config_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=HERE.parents[1]/'.verification') as d:
            path=Path(d)/'config';path.write_text(json.dumps(dict(CONFIG,listen='0.0.0.0')))
            with self.assertRaises(ValueError):p.load(path)
    def test_proxy_environment_is_scoped_and_no_proxy_cannot_bypass_openai(self):
        before=dict(os.environ)
        with patch.dict(os.environ,{'https_proxy':'http://old','NO_PROXY':'*','wss_proxy':'socks5://old'}):
            env=c.proxy_environment({'local_port':18747})
            for k in ['HTTPS_PROXY','https_proxy','WSS_PROXY','wss_proxy','ALL_PROXY']:
                self.assertEqual('http://127.0.0.1:18747',env[k])
            self.assertEqual('localhost,127.0.0.1,::1',env['NO_PROXY'])
            self.assertEqual('*',os.environ['NO_PROXY'])
        self.assertEqual(before,dict(os.environ))
    def test_ssh_does_not_reuse_existing_connections_or_disable_host_checking(self):
        args=c.ssh_command({'ssh':'ssh','identity':'key','known_hosts':'hosts','local_port':18747,'remote_port':18747,'server':'root@host'})
        for value in ['ControlMaster=no','ControlPath=none','StrictHostKeyChecking=yes','ExitOnForwardFailure=yes','ForwardAgent=no','ServerAliveInterval=10','ServerAliveCountMax=3','127.0.0.1:18747:127.0.0.1:18747']:
            self.assertIn(value,args)
    def test_generic_wrapper_passes_original_arguments_without_extra_codex_flags(self):
        args=['client','command','/bin/bash','--noprofile','--norc','-c',"printf '%s' 'kept with spaces'"]
        with patch.object(c,'config',return_value={'local_port':18747}),patch.object(c,'start') as start,patch.object(c.os,'execvpe') as execute,patch.object(sys,'argv',args),contextlib.redirect_stderr(io.StringIO()):
            c.main();start.assert_called_once();actual=execute.call_args.args;self.assertEqual('/bin/bash',actual[0]);self.assertEqual(args[2:],actual[1]);self.assertEqual('http://127.0.0.1:18747',actual[2]['HTTPS_PROXY'])
    def test_failed_preflight_never_executes_the_agent(self):
        with patch.object(c,'config',return_value={'local_port':18747}),patch.object(c,'start',side_effect=RuntimeError('fixture unreachable')),patch.object(c.os,'execvpe') as execute,patch.object(sys,'argv',['client','command','codex']):
            with self.assertRaises(RuntimeError):c.main()
            execute.assert_not_called()

class Networking(unittest.IsolatedAsyncioTestCase):
    async def test_geohide_failover_never_uses_normal_dns(self):
        calls=[]
        async def connect(ip,port):
            calls.append(ip)
            if len(calls)==1:raise OSError('unavailable')
            return 'reader','writer'
        async def dns(*args,**kwargs):self.fail('mapped domain leaked to DNS')
        relay=p.Relay(CONFIG,connect,dns)
        self.assertEqual(('reader','writer','geohide'),await relay.connect('api.openai.com'))
        self.assertEqual(CONFIG['geohide']['api.openai.com'],calls)
    async def test_all_geo_failures_do_not_fall_back_to_direct(self):
        async def connect(*_):raise OSError('down')
        relay=p.Relay(CONFIG,connect)
        with self.assertRaises(ConnectionError):await relay.connect('api.openai.com')
    async def test_normal_dns_checks_resolved_ips_before_connecting(self):
        async def dns(*_,**kw):return [(2,1,6,'',('169.254.169.254',443))]
        async def connect(*_):self.fail('private address connected')
        with self.assertRaises(ValueError):await p.Relay(CONFIG,connect,dns).connect('metadata.example')
    async def test_opaque_duplex_has_no_payload_or_auth_logging(self):
        async def echo(r,w):
            w.write(await r.read(1024));await w.drain();w.close()
        upstream=await asyncio.start_server(echo,'127.0.0.1',0)
        async def connect(*_):return await asyncio.open_connection('127.0.0.1',upstream.sockets[0].getsockname()[1])
        relay=p.Relay(CONFIG,connect);server=await asyncio.start_server(relay.handle,'127.0.0.1',0)
        log=io.StringIO()
        try:
            with contextlib.redirect_stdout(log):
                r,w=await asyncio.open_connection('127.0.0.1',server.sockets[0].getsockname()[1])
                w.write(b'CONNECT api.openai.com:443 HTTP/1.1\r\nAuthorization: fixture-secret\r\n\r\n');await w.drain()
                self.assertIn(b'200',await r.readuntil(b'\r\n\r\n'))
                w.write(b'opaque-TLS-fixture-secret');await w.drain();self.assertEqual(b'opaque-TLS-fixture-secret',await r.readexactly(25))
                w.close();await w.wait_closed();await asyncio.sleep(.05)
            self.assertNotIn('secret',log.getvalue());self.assertNotIn('Authorization',log.getvalue());self.assertIn('api.openai.com',log.getvalue())
        finally:
            server.close();upstream.close();await server.wait_closed();await upstream.wait_closed()
    async def test_health_and_invalid_methods_do_not_open_upstream(self):
        async def connect(*_):self.fail('unexpected connection')
        relay=p.Relay(CONFIG,connect);server=await asyncio.start_server(relay.handle,'127.0.0.1',0)
        try:
            for request,code in [(b'GET /health HTTP/1.1\r\n\r\n',b'200'),(b'GET https://example.com HTTP/1.1\r\n\r\n',b'405'),(b'CONNECT api.openai.com:80 HTTP/1.1\r\n\r\n',b'400')]:
                r,w=await asyncio.open_connection('127.0.0.1',server.sockets[0].getsockname()[1]);w.write(request);await w.drain();self.assertIn(code,(await r.read()).split(b'\r\n')[0]);w.close();await w.wait_closed()
        finally:server.close();await server.wait_closed()

class ControlFlow(unittest.TestCase):
    def test_authenticated_tcp_control_works_across_processes_and_rejects_stop_without_key(self):
        with tempfile.TemporaryDirectory(dir=HERE.parents[1]/'.verification') as directory:
            base=Path(directory)
            with socket.socket() as probe:
                probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
            source="""import importlib.util,pathlib,sys
spec=importlib.util.spec_from_file_location('client',sys.argv[1]);c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)
c.BASE=pathlib.Path(sys.argv[2]);c.STATE=c.BASE/'state';c.CONTROL=c.STATE/'unused.sock';c.KEY=c.STATE/'control.key'
c.ssh_command=lambda config:[sys.executable,'-c','import time;time.sleep(30)']
c.healthy=lambda config:True
c.supervise({'control_port':int(sys.argv[3])})
"""
            child=subprocess.Popen([sys.executable,'-B','-c',source,str(HERE/'client.py'),str(base),str(port)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True)
            def request(token,op):
                with socket.create_connection(('127.0.0.1',port),timeout=1) as stream:
                    stream.sendall((json.dumps({'token':token,'op':op})+'\n').encode());return json.loads(stream.recv(4096))
            try:
                deadline=time.monotonic()+5;ready=False
                while time.monotonic()<deadline:
                    try:
                        secret=(base/'state/control.key').read_text();ready=request(secret,'status').get('ready')
                        if ready:break
                    except (OSError,ValueError):pass
                    time.sleep(.1)
                self.assertTrue(ready,'manager failed to answer another process')
                self.assertEqual({'error':'unauthorized'},request('invalid','stop'))
                self.assertTrue(request(secret,'status')['ready']);self.assertEqual('stopping',request(secret,'stop')['phase']);self.assertEqual(0,child.wait(timeout=5))
            finally:
                if child.poll() is None:
                    os.killpg(child.pid,signal.SIGTERM);child.wait(timeout=5)

if __name__=='__main__':unittest.main()
