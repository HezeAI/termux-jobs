"""Window-property readiness and late success; no live X11 commands."""
import contextlib,importlib.util,io,subprocess,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('ready',ROOT/'x11/src/main/assets/x11-ready.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ReadyTests(unittest.TestCase):
 def fixture(self,args,deadline=None):
  if args[0]=='dbus-send':return None
  if args[:2]==['xprop','-root']:return '_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x1\n_NET_CLIENT_LIST(WINDOW): window id # 0x2, 0x3\n'
  if args==['xprop','-id','0x1','_NET_SUPPORTING_WM_CHECK']:return '_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x1\n'
  if args[:3]==['xprop','-id','0x2']:return '_NET_WM_WINDOW_TYPE(ATOM) = _NET_WM_WINDOW_TYPE_DOCK\n'
  if args[:3]==['xprop','-id','0x3']:return '_NET_WM_WINDOW_TYPE(ATOM) = _NET_WM_WINDOW_TYPE_DESKTOP\n'
  if args==['xwininfo','-root','-tree']:return ''
  if args[0]=='xwininfo':return 'xwininfo: Window id: 0x2 "Любое имя"\n  Map State: IsViewable\n'
  raise AssertionError(args)
 def test_ready_uses_window_types_and_viewability_not_names_or_language(self):
  with patch.object(m,'query',side_effect=self.fixture) as run:
   self.assertEqual((True,True,True,'ok'),m.snapshot())
   self.assertFalse(any('WM_NAME' in x or 'WM_CLASS' in x for c in run.call_args_list for x in c.args[0]))
 def test_override_redirect_clients_can_be_found_without_matching_titles(self):
  def fake(args,deadline=None):
   if args[:2]==['xprop','-root']:return '_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x1\n_NET_CLIENT_LIST(WINDOW): window id # 0x5\n'
   if args==['xprop','-id','0x5','_NET_WM_WINDOW_TYPE']:return '_NET_WM_WINDOW_TYPE(ATOM) = _NET_WM_WINDOW_TYPE_NORMAL\n'
   if args==['xwininfo','-root','-tree']:return '0x2 "Локализованная панель": ("xfce4-panel" "Xfce4-panel")\n0x3 "Рабочая область": ("xfdesktop" "Xfdesktop")\n'
   return self.fixture(args,deadline)
  with patch.object(m,'query',side_effect=fake):self.assertEqual((True,True,True,'ok'),m.snapshot())
 def test_unmapped_desktop_is_not_ready(self):
  def fake(args,deadline=None):
   if args[:3]==['xwininfo','-id','0x3']:return 'Map State: IsUnMapped\n'
   return self.fixture(args,deadline)
  with patch.object(m,'query',side_effect=fake):self.assertEqual((True,True,False,'ok'),m.snapshot())
 def test_missing_or_mismatched_window_manager_cannot_complete(self):
  def fake(args,deadline=None):
   if args==['xprop','-id','0x1','_NET_SUPPORTING_WM_CHECK']:return '_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x4\n'
   return self.fixture(args,deadline)
  with patch.object(m,'query',side_effect=fake):self.assertFalse(m.snapshot()[0])
 def test_authentication_or_command_failure_is_not_a_missing_package(self):
  with patch.object(m,'query',return_value=None):self.assertEqual((False,False,False,'query-failed'),m.snapshot())
 def test_late_success_after_timeout_is_reported_once_and_stops_polling(self):
  now=[0];calls=[0]
  def probe():calls[0]+=1;return now[0]>=25,True,True,'ok'
  def sleep(seconds):now[0]+=seconds
  log=io.StringIO()
  with contextlib.redirect_stdout(log):self.assertTrue(m.wait('token',probe,lambda:now[0],sleep))
  self.assertEqual(1,log.getvalue().count('DESKTOP_TIMEOUT'));self.assertEqual(1,log.getvalue().count('DESKTOP_READY'));self.assertTrue(now[0]<30)
 def test_never_ready_probe_is_bounded_and_warns_once(self):
  now=[0]
  def sleep(seconds):now[0]+=seconds
  log=io.StringIO()
  with contextlib.redirect_stdout(log):self.assertFalse(m.wait('token',lambda:(True,True,False,'ok'),lambda:now[0],sleep))
  self.assertTrue(180<=now[0]<=182);self.assertEqual(1,log.getvalue().count('DESKTOP_TIMEOUT'));self.assertNotIn('DESKTOP_READY',log.getvalue())
 def test_stage_markers_emit_once_without_extra_probe_calls(self):
  now=[0];calls=[0]
  def probe():calls[0]+=1;return True,now[0]>=1,now[0]>=2,'ok'
  def sleep(seconds):now[0]+=seconds
  log=io.StringIO()
  with contextlib.redirect_stdout(log):self.assertTrue(m.wait('token',probe,lambda:now[0],sleep))
  self.assertEqual(5,calls[0])
  for stage in ['wm-observed','panel-visible','desktop-visible']:self.assertEqual(1,log.getvalue().count('JOBS_X11_STARTUP='+stage))
 def test_timed_out_child_is_bounded_and_does_not_leak_output(self):
  with patch.object(m.subprocess,'run',side_effect=subprocess.TimeoutExpired(['xprop'],2)):self.assertIsNone(m.query(['xprop']))

 def test_xfce_idle_completes_without_assuming_panel_or_desktop_types(self):
  def fake(args,deadline=None):
   if args[0]=='dbus-send':return 'method return time=1 sender=:1.2 -> destination=:1.3\n   uint32 1\n'
   return self.fixture(args,deadline)
  with patch.object(m,'query',side_effect=fake) as calls:
   value=m.snapshot();self.assertEqual((True,False,False,'session-idle'),value)
   self.assertFalse(any(c.args[0][0]=='xwininfo' for c in calls.call_args_list))
  log=io.StringIO()
  with contextlib.redirect_stdout(log):self.assertTrue(m.wait('token',lambda:value))
  self.assertIn('DESKTOP_SOURCE token session-idle',log.getvalue());self.assertIn('DESKTOP_READY token',log.getvalue())
 def test_starting_shutdown_malformed_or_unavailable_bus_do_not_report_ready(self):
  for reply in [None,'uint32 0','uint32 2','uint32 3','uint32 4','string "uint32 1"','uint32 10']:
   with patch.object(m,'query',return_value=reply):self.assertFalse(m.session_idle(m.time.monotonic()+3))
 def test_idle_session_cannot_bypass_missing_or_wrong_window_manager(self):
  def fake(args,deadline=None):
   if args==['xprop','-id','0x1','_NET_SUPPORTING_WM_CHECK']:return '_NET_SUPPORTING_WM_CHECK(WINDOW): window id # 0x4\n'
   if args[0]=='dbus-send':return 'uint32 1\n'
   return self.fixture(args,deadline)
  with patch.object(m,'query',side_effect=fake):
   state=m.snapshot();self.assertFalse(state[0]);self.assertNotEqual('session-idle',state[3])
