"""Private socket, cookie and owned process lifetime. Never launch real PulseAudio."""
import os,subprocess,tempfile,time,unittest,select
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
HELPER=ROOT/'x11/src/main/assets/x11-audio.sh'
GUARD=ROOT/'x11/src/main/assets/x11-guardian.sh'
class AudioTests(unittest.TestCase):
 def fixture(self,d):
  base=Path(d);prefix=base/'usr';bin=prefix/'bin';bin.mkdir(parents=True);control=base/'workspace-x11';control.mkdir()
  modules=prefix/'lib/pulseaudio/modules';modules.mkdir(parents=True)
  for name in ['module-sles-sink.so','module-native-protocol-unix.so']:(modules/name).touch()
  pulse=bin/'pulseaudio';pulse.write_text('''#!/usr/bin/python3
import os,socket,sys,time
from pathlib import Path
Path(os.environ['ARGS']).write_text('\\n'.join(sys.argv[1:]))
Path(os.environ['PIDFILE']).write_text(str(os.getpid()))
arg=next(a for a in sys.argv if a.startswith('--load=module-native-protocol-unix '))
path=next(p[7:] for p in arg.split() if p.startswith('socket='))
s=socket.socket(socket.AF_UNIX);s.bind(path);s.listen(1)
time.sleep(30)
''');pulse.chmod(0o755)
  env={**os.environ,'PREFIX':str(prefix),'ARGS':str(base/'args'),'PIDFILE':str(base/'pid')}
  return control,env
 def test_owned_audio_uses_cookie_without_tcp_and_eof_stops_only_its_group(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);sentinel=subprocess.Popen(['sleep','30'])
   command='source "$1"; control=$2; jobs_audio_start; printf "guest=%s\\n" "$jobs_audio_guest"; sleep 30'
   p=subprocess.Popen(['setsid','bash',str(GUARD),'bash','-c',command,'fixture',str(HELPER),str(control)],env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
   pid=None
   try:
    for _ in range(100):
     f=Path(env['PIDFILE'])
     if f.exists() and f.read_text().isdigit():pid=int(f.read_text());break
     time.sleep(.02)
    self.assertIsNotNone(pid)
    for _ in range(100):
     dirs=list(control.glob('pulse.*'))
     if dirs and (dirs[0]/'native').exists():break
     time.sleep(.02)
    audio=dirs[0];self.assertEqual(256,len((audio/'cookie').read_bytes()));self.assertEqual(0o600,(audio/'cookie').stat().st_mode&0o777)
    args=Path(env['ARGS']).read_text();self.assertIn('--daemonize=no',args);self.assertIn('--use-pid-file=no',args);self.assertIn('auth-anonymous=no',args);self.assertIn('auth-cookie-enabled=yes',args)
    argv=args.splitlines();self.assertFalse(any('module-native-protocol-tcp' in a or 'module-sles-source' in a for a in argv));self.assertNotIn('--kill',argv)
    readable,_,_=select.select([p.stdout],[],[],3);self.assertTrue(readable,'audio readiness acknowledgement missing');self.assertIn(b'JOBS_X11_AUDIO=ready',p.stdout.readline())
    p.stdin.close();p.wait(timeout=5);p.stdout.read();self.assertIsNone(sentinel.poll())
    stat=Path(f'/proc/{pid}/stat');self.assertTrue(not stat.exists() or stat.read_text().split(') ',1)[1].startswith('Z '))
    self.assertFalse(audio.exists())
   finally:
    if p.poll() is None:p.stdin.close();p.wait(timeout=5)
    p.stdout.close();sentinel.terminate();sentinel.wait()
 def test_missing_modules_never_launch_a_host_daemon_and_leave_explicit_unavailable_route(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   control,env=self.fixture(d);(Path(env['PREFIX'])/'lib/pulseaudio/modules/module-sles-sink.so').unlink()
   p=subprocess.run(['bash','-c','source "$1"; control=$2; jobs_audio_start; echo "$jobs_audio_guest"','fixture',str(HELPER),str(control)],env=env,capture_output=True,timeout=5)
   self.assertEqual(0,p.returncode);self.assertIn(b'missing-components',p.stdout);self.assertIn(b'audio-unavailable',p.stdout);self.assertFalse(Path(env['PIDFILE']).exists())
