"""Metadata recorder and wallpaper failures, no real display or user rootfs."""
import importlib.util
import io
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
def load(name):
 p=ROOT/('third-party/x11-diagnostics' if name=='observe' else 'x11/src/main/assets')/('x11-'+name+'.py')
 spec=importlib.util.spec_from_file_location('x11_'+name,p);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod
observe=load('observe');wallpaper=load('wallpaper')

class ObserveTests(unittest.TestCase):
 def test_hide_remap_does_not_count_as_recreation(self):
  r=observe.Recorder();r.sample(5,{10:True});r.event(18,10);r.sample(5,{10:False});r.event(19,10);r.sample(5,{10:True})
  self.assertEqual(1,r.counts['popup-new']);self.assertEqual(0,r.counts['popup-destroy']);self.assertEqual(0,r.counts['focus-change']);self.assertEqual(1,r.counts['popup-hide']);self.assertEqual(1,r.counts['popup-map'])
 def test_destroy_new_and_focus_loss_are_separate(self):
  r=observe.Recorder();r.sample(5,{10:True});r.event(17,10);r.sample(1,{});r.event(16,20);r.sample(5,{20:True})
  self.assertEqual(2,r.counts['popup-new']);self.assertEqual(1,r.counts['popup-destroy']);self.assertEqual(2,r.counts['focus-change']);self.assertIn('destroy:a',r.line(2));self.assertIn('new:14',r.line(2))
 def test_bounded_history_and_no_event_payload_fields(self):
  r=observe.Recorder()
  for i in range(6000):r.sample(i,{i:True})
  self.assertLessEqual(len(r.seen),4096);self.assertLessEqual(len(r.transitions),8);self.assertLess(len(r.line(1)),900)
  self.assertEqual(192,observe.C.sizeof(observe.Event));self.assertEqual(136,observe.C.sizeof(observe.Attr));self.assertEqual(120,observe.Attr.override_redirect.offset)
 def test_missing_display_is_a_sanitized_failure(self):
  env={**os.environ,'DISPLAY':'invalid-display','XAUTHORITY':'/do-not-print-secret'}
  result=subprocess.run(['python3',str(ROOT/'third-party/x11-diagnostics/x11-observe.py')],env=env,text=True,capture_output=True,timeout=5)
  self.assertEqual(0,result.returncode);self.assertIn('unavailable=',result.stdout);self.assertNotIn('do-not-print',result.stdout);self.assertNotIn('Traceback',result.stderr)

class WallpaperTests(unittest.TestCase):
 def test_never_replace_custom_images_even_unavailable(self):
  with patch.object(wallpaper.os.path,'isfile',return_value=False):
   self.assertTrue(wallpaper.replacement(wallpaper.DEFAULT));self.assertTrue(wallpaper.replacement(None))
   self.assertFalse(wallpaper.replacement('/sdcard/Pictures/chosen.png'))
 def test_settings_failure_does_not_write(self):
  calls=[]
  def query(*args):calls.append(args);return None
  with patch.object(wallpaper,'query',side_effect=query),redirect_stdout(io.StringIO()) as out:wallpaper.main()
  self.assertIn('settings-unavailable',out.getvalue());self.assertFalse(any('-s' in x for x in calls))
 def test_selected_image_preserved_and_missing_defaults_only_repaired(self):
  current='/backdrop/screen0/monitorscreen/workspace0/last-image';chosen='/sdcard/private-chosen-image.png';writes=[]
  def query(*args):
   if args==('-l',):return current
   if '-s' in args:writes.append(args);return ''
   if args==('-p',current):return chosen
   return None
  with patch.object(wallpaper,'query',side_effect=query),patch.object(wallpaper.os.path,'isfile',return_value=True),patch.object(wallpaper,'decode',return_value='ok-64x64'),redirect_stdout(io.StringIO()) as out:wallpaper.main()
  self.assertFalse(any(current in args for args in writes));self.assertNotIn(chosen,out.getvalue())
 def test_failed_property_read_does_not_overwrite_listed_property(self):
  current='/backdrop/screen0/monitorscreen/workspace0/last-image';writes=[]
  def query(*args):
   if args==('-l',):return current
   if '-s' in args:writes.append(args);return ''
   return None
  with patch.object(wallpaper,'query',side_effect=query),patch.object(wallpaper.os.path,'isfile',return_value=True),redirect_stdout(io.StringIO()):wallpaper.main()
  self.assertFalse(any(current in args for args in writes))
 def test_absent_builtin_default_repaired_but_custom_choice_retained(self):
  current='/backdrop/screen0/monitorscreen/workspace0/last-image';writes=[]
  def query(*args):
   if args==('-l',):return current
   if '-s' in args:writes.append(args);return ''
   if args==('-p',current):return wallpaper.DEFAULT
   return None
  with patch.object(wallpaper,'query',side_effect=query),patch.object(wallpaper.os.path,'isfile',side_effect=lambda p:p==wallpaper.FALLBACK),patch.object(wallpaper,'decode',return_value='ok-64x64'),redirect_stdout(io.StringIO()) as out:wallpaper.main()
  self.assertEqual(4,len(writes));self.assertTrue(any(current in args for args in writes));self.assertIn('fallback=applied',out.getvalue());self.assertNotIn(wallpaper.DEFAULT,out.getvalue())
 def test_legacy_selection_copied_only_to_absent_screen_setting(self):
  old='/backdrop/screen0/monitorTermuxJobsX11/workspace0/last-image';new='/backdrop/screen0/monitorscreen/workspace0/last-image';chosen='/sdcard/chosen-private.png';writes=[]
  def query(*args):
   if args==('-l',):return old
   if '-s' in args:writes.append(args);return ''
   if args==('-p',old):return chosen
   return None
  with patch.object(wallpaper,'query',side_effect=query),patch.object(wallpaper.os.path,'isfile',side_effect=lambda p:p in (chosen,wallpaper.FALLBACK)),patch.object(wallpaper.os,'access',return_value=True),patch.object(wallpaper,'decode',return_value='ok-64x64'),redirect_stdout(io.StringIO()) as out:wallpaper.main()
  self.assertTrue(any(args[:4]==('-p',new,'-s',chosen) for args in writes));self.assertFalse(any(old in args for args in writes));self.assertIn('selection-restored=true',out.getvalue());self.assertNotIn(chosen,out.getvalue())
 def test_decode_fallback_with_real_gdk_if_available(self):
  result=wallpaper.decode(str(ROOT/'x11/src/main/assets/x11-default-wallpaper.png'))
  if result=='decoder-unavailable':self.skipTest('GdkPixbuf unavailable on test host')
  self.assertEqual('ok-64x64',result)
 def test_invalid_and_missing_images_are_not_successful(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as directory:
   p=Path(directory)/'bad.png';p.write_bytes(b'not-a-png')
   self.assertNotEqual('ok-64x64',wallpaper.decode(str(p)))
   self.assertEqual('unreadable',wallpaper.decode(str(p.parent/'missing.png')))
if __name__=='__main__':unittest.main()

class ManualObservationTests(unittest.TestCase):
 def test_control_identity_size_and_symlink_checks(self):
  token='a11a11a1-1111-4111-8111-111111111111'
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as directory:
   path=Path(directory)/'control';path.write_text(token+' 9 on\n')
   self.assertEqual((9,True),observe.read_control(token,str(path)));self.assertIsNone(observe.read_control('other',str(path)))
   path.write_text(token+' 99999999999999999999 on\n');self.assertIsNone(observe.read_control(token,str(path)))
   path.write_text('x'*300);self.assertIsNone(observe.read_control(token,str(path)))
   link=Path(directory)/'link';link.symlink_to(path);self.assertIsNone(observe.read_control(token,str(link)))
 def test_property_monitor_never_returns_image_path_or_value(self):
  line="Property '/backdrop/screen0/monitorscreen/workspace0/last-image' changed to '/sdcard/private/photo.jpg'"
  result=observe.backdrop_key(line);self.assertIn('key=last-image',result);self.assertIn('workspace=0',result);self.assertNotIn('photo',result);self.assertNotIn('/sdcard',result)
  self.assertIsNone(observe.backdrop_key('/unrelated/private-data secret'))
 def test_read_only_snapshot_does_not_repair_missing_default(self):
  calls=[]
  def query(*args):calls.append(args);return '' if args==('-l',) else None
  with patch.object(wallpaper,'query',side_effect=query),patch.object(wallpaper.os.path,'isfile',return_value=True),patch.object(wallpaper,'decode',return_value='ok-64x64'),redirect_stdout(io.StringIO()) as out:wallpaper.main(repair=False)
  self.assertFalse(any('-s' in args for args in calls));self.assertIn('current-snapshot',out.getvalue())
 def test_controller_can_restart_without_deadline_and_closes_own_monitor(self):
  token='a11a11a1-1111-4111-8111-111111111111';writes=[];closed=[];runs=[]
  class Client:
   def run(self,duration,keep_running,stamp,monitor):runs.append(duration)
  class Monitor:
   def __init__(self,stamp):pass
   def close(self):closed.append(True)
  sequence=iter([(0,False),(1,True),(2,False),(3,True)])
  with patch.dict(observe.os.environ,{'JOBS_X11_FOCUS_TOKEN':token}),patch.object(observe,'read_control',side_effect=lambda t:next(sequence)),patch.object(observe,'X11',Client),patch.object(observe,'BackdropMonitor',Monitor),patch.object(observe.time,'sleep'),patch.object(observe.os,'write',side_effect=lambda fd,b:writes.append(b.decode())):
   with self.assertRaises(StopIteration):observe.controlled()
  self.assertEqual([None,None],runs);self.assertEqual(2,len(closed));self.assertTrue(any(' 2 stopped' in value for value in writes));self.assertTrue(any(' 3 active' in value for value in writes))
 def test_pointer_button_transitions_are_metadata_only(self):
  r=observe.Recorder();r.pointer(0,10);r.pointer(1,10);r.pointer(0,12)
  self.assertEqual(2,r.counts['mouse-button-change']);self.assertIn('buttons=0',r.line(3));self.assertIn('pointer-child=c',r.line(3))

class BackdropBufferTests(unittest.TestCase):
 def test_monitor_delivers_line_before_child_exit_and_never_emits_value(self):
  import time,sys
  real_popen=subprocess.Popen
  def launch(args,**kwargs):
   self.assertEqual('xfconf-query',args[0])
   return real_popen([sys.executable,'-c',"import time; print('set: /backdrop/screen0/monitorscreen/workspace0/last-image private-photo'); time.sleep(5)"],**kwargs)
  with patch.object(observe.subprocess,'Popen',side_effect=launch),redirect_stdout(io.StringIO()) as out:
   monitor=observe.BackdropMonitor(lambda text:text);monitor.next_snapshot=time.monotonic()+100
   try:
    deadline=time.monotonic()+2
    while time.monotonic()<deadline and 'key=last-image' not in out.getvalue():monitor.tick();time.sleep(.02)
    self.assertIn('key=last-image',out.getvalue());self.assertNotIn('private-photo',out.getvalue());self.assertIsNone(monitor.monitor.poll())
   finally:monitor.close()

class ContentObservationTests(unittest.TestCase):
 def test_damage_tracks_only_bounded_owned_popup_regions_and_rearms_notifications(self):
  from types import SimpleNamespace
  created=[];destroyed=[];subtracted=[]
  client=SimpleNamespace(display=1,window_types={},flush=lambda d:None)
  def classify(window):client.window_types[window]='popup_menu/panel';return client.window_types[window]
  client.classify=classify
  watch=observe.DamageWatch.__new__(observe.DamageWatch);watch.client=client;watch.objects={};watch.available=True;watch.event_base=90
  watch.create=lambda d,w,level:created.append((w,level)) or w+1000
  watch.destroy=lambda d,region:destroyed.append(region)
  watch.subtract=lambda d,region,repair,parts:subtracted.append((region,repair,parts))
  watch.watch({x:True for x in range(1,160)});self.assertEqual(128,len(watch.objects));self.assertEqual(128,len(created));self.assertTrue(all(level==3 for w,level in created))
  event=observe.Event();d=observe.DamageEvent();d.type=90;d.drawable=1;d.damage=1001
  observe.C.memmove(observe.C.byref(event),observe.C.byref(d),observe.C.sizeof(d));r=observe.Recorder()
  self.assertTrue(watch.event(event,r));self.assertEqual(1,r.counts['content-update']);self.assertEqual([(1001,0,0)],subtracted)
  d.damage=999;observe.C.memmove(observe.C.byref(event),observe.C.byref(d),observe.C.sizeof(d));watch.event(event,r);self.assertEqual(1,r.counts['content-update'])
  watch.watch({1:True});self.assertEqual({1:1001},watch.objects);self.assertEqual(127,len(destroyed));watch.close();self.assertEqual(128,len(destroyed));self.assertFalse(watch.objects)
 def test_missing_damage_library_is_explicit_and_does_not_disable_window_observation(self):
  with patch.object(observe.C,'CDLL',side_effect=OSError('no-library')):
   from types import SimpleNamespace
   watch=observe.DamageWatch(SimpleNamespace());self.assertFalse(watch.available);watch.watch({1:True});self.assertFalse(watch.event(observe.Event(),observe.Recorder()));watch.close()
 def test_pointer_counts_changes_without_exposing_coordinates(self):
  r=observe.Recorder();r.pointer(0,1,(12345,67890));r.pointer(0,1,(12346,67890));r.pointer(0,1,(12346,67890))
  self.assertEqual(1,r.counts['pointer-position-change']);self.assertNotIn('12346',r.line(1));self.assertNotIn('67890',r.line(1));self.assertEqual(0,r.counts['mouse-button-change'])
