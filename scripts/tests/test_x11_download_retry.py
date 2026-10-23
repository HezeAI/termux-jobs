"""Real APT acquire retries against loopback fixture only; no package installation."""
import hashlib,http.server,subprocess,tempfile,threading,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class DownloadRetryTests(unittest.TestCase):
 def test_all_desktop_apt_paths_bound_network_retries_without_disabling_trust(self):
  for name in ['x11-desktop.sh','x11-native-desktop.sh','x11-native-programs.py','x11-programs.sh','x11-firefox.py']:
   s=(ROOT/'x11/src/main/assets'/name).read_text()
   self.assertIn('Acquire::Retries=3',s,name);self.assertIn('Acquire::http::Timeout=30',s,name);self.assertIn('Acquire::https::Timeout=30',s,name)
   self.assertNotIn('--allow-unauthenticated',s);self.assertNotIn('Verify-Peer=false',s)
 def test_real_apt_retries_transient_http_error_but_does_not_retry_bad_digest(self):
  helper=Path('/usr/lib/apt/apt-helper')
  if not helper.exists():self.skipTest('local APT helper unavailable')
  data=b'isolated fixture, not a package';requests=[]
  class Handler(http.server.BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):
    requests.append(self.path)
    if self.path=='/retry' and requests.count('/retry')<3:self.send_error(503);return
    self.send_response(200);self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
  server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
  try:
   with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
    for path,digest,ok,count in [('/retry',hashlib.sha256(data).hexdigest(),True,3),('/bad','0'*64,False,1)]:
     dest=Path(d)/path[1:]
     p=subprocess.run([str(helper),'-o','Acquire::Retries=3','-o','Acquire::http::Timeout=3','-o','Acquire::http::Proxy=DIRECT','-o','Acquire::http::Proxy::127.0.0.1=DIRECT','download-file','http://127.0.0.1:'+str(server.server_port)+path,str(dest),'SHA256:'+digest],capture_output=True,text=True,timeout=20)
     self.assertEqual(ok,p.returncode==0,p.stderr);self.assertEqual(count,requests.count(path))
     if ok:self.assertEqual(data,dest.read_bytes())
  finally:server.shutdown();server.server_close();thread.join(3)
