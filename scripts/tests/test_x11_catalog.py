"""Catalog filtering without installing libraries or loading a real AppStream pool."""
import importlib.util,unittest
from pathlib import Path
from types import SimpleNamespace
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('catalog',ROOT/'x11/src/main/assets/x11-catalog.py');catalog=importlib.util.module_from_spec(spec);spec.loader.exec_module(catalog)
class CatalogTests(unittest.TestCase):
 def component(self,pkg,name='Drawing App',summary='Draw pictures'):
  return SimpleNamespace(get_pkgnames=lambda:[pkg],get_name=lambda:name,get_summary=lambda:summary)
 def package(self,installed=False,available=True,snap=False):
  return SimpleNamespace(candidate=SimpleNamespace(record={'Depends':'snapd' if snap else 'libc6'}) if available else None,is_installed=installed,_pkg=SimpleNamespace(current_state=6 if installed else 0))
 def test_only_native_apt_packages_are_listed_and_snap_is_not_installable(self):
  values=[self.component('good-app'),self.component('snap-app'),self.component('absent'),self.component('--option'),self.component('other-app'),self.component('unpacked-app')]
  pool=SimpleNamespace(get_components_by_kind=lambda kind:SimpleNamespace(as_array=lambda:values))
  cache={'good-app':self.package(True),'snap-app':self.package(snap=True),'other-app':self.package(available=False),'unpacked-app':self.package()}
  result={e['package']:e for e in catalog.collect(pool,cache,'desktop',6)}
  self.assertEqual({'good-app','snap-app','unpacked-app'},set(result));self.assertTrue(result['good-app']['installed']);self.assertFalse(result['snap-app']['available']);self.assertFalse(result['unpacked-app']['installed'])
 def test_older_list_api_deduplicates_sanitizes_and_bounds_descriptions(self):
  values=[self.component('same-app',summary='short'),self.component('same-app',name='<b>Имя</b>\nпрограммы',summary='<p>'+'x'*900+'</p>')]
  pool=SimpleNamespace(get_components_by_kind=lambda kind:values)
  result=catalog.collect(pool,{'same-app':self.package()},'desktop',6)
  self.assertEqual(1,len(result));self.assertEqual('Имя программы',result[0]['name']);self.assertEqual(500,len(result[0]['summary']))

class InstalledTests(unittest.TestCase):
 def test_installed_desktop_files_are_owned_configured_localized_and_never_executed(self):
  import tempfile
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   directory=Path(d);calls=[]
   entries={'mousepad':'[Desktop Entry]\nType=Application\nName=Mousepad\nName[ru]=Редактор\nComment=Text editor\nExec=touch /tmp/never-execute\n', 'hidden-app':'[Desktop Entry]\nType=Application\nName=Hidden\nNoDisplay=true\n', 'unconfigured':'[Desktop Entry]\nType=Application\nName=Incomplete\n', 'unowned':'[Desktop Entry]\nType=Application\nName=Not a package\n'}
   for pkg,text in entries.items():(directory/(pkg+'.desktop')).write_text(text)
   def output(args,text):
    calls.append(args)
    if '-W' in args:return 'mousepad\tdeinstall ok installed\nhidden-app\tinstall ok installed\nunconfigured\tinstall ok unpacked\n'
    return ''.join(pkg+': '+str(directory/(pkg+'.desktop'))+'\n' for pkg in ['mousepad','hidden-app','unconfigured'])
   result=catalog.installed_programs('ru',directory,output)
   self.assertEqual(['mousepad'],[entry['package'] for entry in result]);self.assertEqual('Редактор',result[0]['name']);self.assertTrue(result[0]['installed']);self.assertEqual(2,len(calls));self.assertTrue(all(args[0]=='dpkg-query' for args in calls))
 def test_empty_directory_does_not_require_apt_or_appstream(self):
  import tempfile
  with tempfile.TemporaryDirectory(dir=ROOT/'.verification') as d:
   self.assertEqual([],catalog.installed_programs('en',Path(d),lambda *a,**kw:self.fail('unexpected process')))
