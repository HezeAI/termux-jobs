"""Metadata-only keyboard gate. No real desktop or user bus is touched."""
import importlib.util,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('jobs_focus',ROOT/'x11/src/main/assets/x11-focus.py');f=importlib.util.module_from_spec(spec);spec.loader.exec_module(f)
class Focus(unittest.TestCase):
 def setUp(self):
  self.now=1.;self.window=(12,55,True);self.events=[];self.g=f.Gate(lambda:self.window,lambda sender:55 if sender==':1.3' else 66,lambda *args:self.events.append(args),lambda:self.now)
 def event(self,kind,cap=None,sender=':1.3',path='/ic/1'):self.g.event(sender,path,kind,cap)
 def focus(self,cap=0):self.event('SetCapability',cap);self.event('FocusIn');self.now+=.21;self.g.due()
 def test_editable_context_opens_once_and_property_noise_does_not_reopen(self):
  self.focus();self.g.changed();self.g.changed();self.assertEqual([(':1.3','/ic/1','FocusIn')],self.events)
 def test_empty_desktop_dock_hidden_or_minimized_context_cannot_open(self):
  for window in [(0,0,False),(12,55,False)]:
   self.window=window;self.focus();self.assertFalse(self.events)
 def test_minimizing_and_switching_to_other_normal_window_clears_stale_focus(self):
  self.focus();self.window=(13,55,True);self.g.changed();self.assertEqual('FocusOut',self.events[-1][-1]);self.g.refresh();self.assertEqual(2,len(self.events))
  self.window=(12,55,True);self.g.changed();self.assertEqual(2,len(self.events));self.focus();self.window=(12,55,False);self.g.changed();self.assertEqual('FocusOut',self.events[-1][-1])
 def test_other_process_or_missing_capability_rejects_context(self):
  self.event('FocusIn');self.now+=1;self.g.due();self.assertFalse(self.events)
  self.window=(12,77,True);self.focus();self.assertFalse(self.events)
 def test_disabled_toolkit_context_and_no_screen_keyboard_flag_rejected(self):
  self.focus(1<<40);self.assertFalse(self.events);self.focus(1<<15);self.assertFalse(self.events)
  self.focus();self.event('SetCapability',1<<40);self.assertEqual('FocusOut',self.events[-1][-1]);self.event('SetCapability',0);self.assertEqual('FocusIn',self.events[-1][-1])
 def test_wm_activation_can_follow_focus_but_cannot_revive_old_context(self):
  self.window=(0,0,False);self.event('SetCapability',0);self.event('FocusIn');self.g.changed();self.assertFalse(self.events)
  self.window=(12,55,True);self.g.changed();self.assertFalse(self.events);self.now+=.21;self.g.due();self.assertEqual('FocusIn',self.events[-1][-1])
 def test_destroy_and_stale_focusout_from_other_context(self):
  self.focus();self.event('FocusOut',path='/other');self.assertEqual(1,len(self.events));self.event('DestroyIC');self.assertEqual('FocusOut',self.events[-1][-1]);self.assertNotIn((':1.3','/ic/1'),self.g.contexts)
 def test_expired_background_context_does_not_reappear_on_later_activation(self):
  self.window=(0,0,False);self.focus();self.window=(12,55,True);self.g.changed();self.assertFalse(self.events)
 def test_context_storage_is_bounded(self):
  for i in range(1000):self.event('SetCapability',0,path='/ic/'+str(i))
  self.assertEqual(256,len(self.g.contexts))
 def test_x11_reader_rejects_desktop_hidden_and_iconic_windows(self):
  from unittest.mock import Mock
  x=f.X11.__new__(f.X11);x.lib=Mock();x.d=None;x.root=1;x.active=0;x.atoms={name:i+1 for i,name in enumerate(('_NET_ACTIVE_WINDOW','_NET_WM_PID','_NET_WM_WINDOW_TYPE','_NET_WM_STATE','_NET_WM_STATE_HIDDEN','WM_STATE','_NET_WM_WINDOW_TYPE_NORMAL','_NET_WM_WINDOW_TYPE_DIALOG','_NET_WM_WINDOW_TYPE_UTILITY'))}
  props={'_NET_ACTIVE_WINDOW':[12],'_NET_WM_PID':[55],'_NET_WM_WINDOW_TYPE':[x.atoms['_NET_WM_WINDOW_TYPE_NORMAL']],'_NET_WM_STATE':[],'WM_STATE':[1,0]};asked=[]
  def prop(window,name):asked.append(name);return props.get(name,[])
  x.prop=prop;self.assertEqual((12,55,True),x.window())
  props['_NET_WM_WINDOW_TYPE']=[999];self.assertFalse(x.window()[2]);props['_NET_WM_WINDOW_TYPE']=[];self.assertTrue(x.window()[2])
  props['_NET_WM_STATE']=[x.atoms['_NET_WM_STATE_HIDDEN']];self.assertFalse(x.window()[2]);props['_NET_WM_STATE']=[];props['WM_STATE']=[3,0];self.assertFalse(x.window()[2])
  props['_NET_ACTIVE_WINDOW']=[];self.assertEqual((0,0,False),x.window());self.assertNotIn('_NET_WM_NAME',asked);self.assertNotIn('WM_NAME',asked)
 def test_bus_parser_only_accepts_selected_metadata(self):
  events=[];p=f.BusLines(lambda *args:events.append(args))
  header='method call time=100 sender=:1.3 -> destination=org.fcitx.Fcitx5 serial=3 path=/ic/1; interface=org.fcitx.Fcitx.InputContext1; member='
  p.line(header+'SetCapability');p.line('   uint64 1099511627776');p.line(header+'FocusIn');p.line(header+'CommitString');p.line('   string "private text"');p.line('x'*10000);p.line(header+'FocusOut')
  self.assertEqual([(':1.3','/ic/1','SetCapability',1<<40),(':1.3','/ic/1','FocusIn',None),(':1.3','/ic/1','FocusOut',None)],events)
 def test_real_private_bus_metadata_format_and_owner_pid(self):
  import os,subprocess,tempfile,textwrap
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   script=Path(d)/'probe.py';script.write_text(textwrap.dedent("""
    import ctypes as c,importlib.util,json,os,subprocess,time
    from pathlib import Path
    spec=importlib.util.spec_from_file_location('focus',Path(os.environ['FOCUS_SOURCE']));f=importlib.util.module_from_spec(spec);spec.loader.exec_module(f)
    lib=c.CDLL('libdbus-1.so.3');lib.dbus_bus_get.argtypes=[c.c_int,c.c_void_p];lib.dbus_bus_get.restype=c.c_void_p;lib.dbus_bus_get_unique_name.argtypes=[c.c_void_p];lib.dbus_bus_get_unique_name.restype=c.c_char_p
    connection=lib.dbus_bus_get(0,None);assert connection
    sender=lib.dbus_bus_get_unique_name(connection).decode();assert f.Owners()(sender)==os.getpid()
    matches=["type='method_call',interface='%s',member='%s'"%(f.INTERFACE,m) for m in f.MEMBERS]
    monitor=subprocess.Popen(['dbus-monitor','--session',*matches],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True)
    try:
     time.sleep(.15)
     for member,args in [('SetCapability',['uint64:1099511627776']),('FocusIn',[]),('FocusOut',[]),('CommitString',['string:fixture-private-content'])]:
      subprocess.run(['dbus-send','--session','--print-reply','--dest=org.freedesktop.DBus','/ic/1',f.INTERFACE+'.'+member,*args],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,timeout=2)
     time.sleep(.15)
    finally:monitor.terminate()
    output=monitor.communicate(timeout=2)[0];assert 'fixture-private-content' not in output
    events=[];parser=f.BusLines(lambda *args:events.append(args))
    for line in output.splitlines():parser.line(line)
    assert [v[2] for v in events]==['SetCapability','FocusIn','FocusOut'],events
    assert events[0][3]==1<<40
    print('private-bus metadata and PID: ok')
   """))
   env=dict(os.environ,TMPDIR=d,FOCUS_SOURCE=str(ROOT/'x11/src/main/assets/x11-focus.py'));env.pop('DBUS_SESSION_BUS_ADDRESS',None)
   result=subprocess.run(['dbus-run-session','--','python3','-B',str(script)],env=env,capture_output=True,text=True,timeout=8)
   self.assertEqual(0,result.returncode,result.stderr);self.assertIn('metadata and PID: ok',result.stdout)
 def test_malformed_payload_and_identity_refused(self):
  events=[];p=f.BusLines(lambda *args:events.append(args));header='method call sender=:1.3 -> destination=x path=/ic/1; interface=org.fcitx.Fcitx.InputContext1; member=SetCapability'
  for value in ('   uint64 -1','   uint64 18446744073709551616','   string "123"'):
   p.line(header);p.line(value)
  self.assertFalse(events)
if __name__=='__main__':unittest.main()
