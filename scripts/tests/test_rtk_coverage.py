"""Actual pinned RTK filters; isolated command fixtures, no model/auth/network."""
import importlib.util,json,os,shutil,subprocess,tempfile,unittest,uuid
from pathlib import Path
REPO=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('rtk',REPO/'app/src/main/assets/rtk-mode.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Coverage(unittest.TestCase):
 def test_classification_and_shell_refusal(self):
  for cmd,kind in {'rg -n needle src':'grep','grep -n needle file.txt':'grep','rg --files':'find','find . -name "test.py"':'find','cat file.txt':'read','head -n 200 file.txt':'read','tail -n 200 file.txt':'read','./gradlew test --offline':'build-preview','gradle assembleDebug':'build-preview','python3 -m pytest -q':'pytest','pytest tests':'pytest','cargo test --offline':'cargo-test','go test -json ./pkg':'go-test','go build ./pkg':'go-build','ctest --output-on-failure':'ctest','vitest run':'vitest'}.items():self.assertEqual(m.filter_for(cmd),kind,cmd)
  for cmd in ['cat /tmp/originals/full.txt','rg --json foo .','find . -delete','find . -exec cat {} ;','cat x | head','pytest; id','./gradlew test 2>&1','pytest $(id)','pytest `id`','A=x pytest','cat *.txt','cat ~/file','cat file\\ name','rg "foo\nbar" .','python3 script.py','npm test','go test ./pkg','vitest','tail -f x','pytest --pdb','./gradlew --continuous test']:
   self.assertIsNone(m.filter_for(cmd),cmd)
 def test_each_family_exit_stderr_and_full_output(self):
  with tempfile.TemporaryDirectory(dir=REPO/'.verification/rtk-expand') as tmp:
   root=Path(tmp);home=root/'home';home.mkdir();component=home/'.local/share/termux-jobs/components/rtk'/m.PIN['version'];component.mkdir(parents=True)
   import tarfile
   with tarfile.open(Path(os.environ.get('RTK_TEST_ARCHIVE',str(REPO/'.verification/rtk-remote/download/rtk-oss-1.1.0-jobs.1-aarch64-unknown-linux-gnu.tar.gz')))) as archive:archive.extractall(component,filter='data')
   for name in ['rtk-mode.py','rtk-component.json']:shutil.copy2(REPO/'app/src/main/assets'/name,component/name)
   profile=str(uuid.uuid4());state=home/'.local/share/termux-jobs/rtk/chats'/profile;state.mkdir(parents=True);(state/'mode.json').write_text('{"enabled":true}')
   env={**os.environ,'HOME':str(home)}
   outputs={'grep':''.join(f'source.txt:{n}:needle repeated abcdefghijklmnopqrstuvwxyz\n' for n in range(400)), 'find':''.join(f'./src/file-{n}.py\n' for n in range(400)), 'read':''.join(f'line {n} abcdefghijklmnopqrstuvwxyz\n' for n in range(400)), 'build-preview':''.join(f'> Task :mod{n}:compileJava UP-TO-DATE\n' for n in range(400))+'BUILD SUCCESSFUL in 2s\n400 actionable tasks\n', 'pytest':'============================= test session starts ==============================\n'+''.join(f'tests/test_{n}.py . [ 50%]\n' for n in range(400))+'============================= 400 passed in 1s ==============================\n','cargo-test':''.join(f'test test_{n} ... ok\n' for n in range(400))+'test result: ok. 400 passed; 0 failed; 0 ignored\n','go-test':''.join(json.dumps({'Action':'pass','Package':'fixture','Test':f'Test{n}'})+'\n' for n in range(400)), 'go-build':''.join(f'package/file{n}.go:1:1: diagnostic\n' for n in range(400)), 'ctest':''.join(f'{n}/400 Test #{n}: test_{n} .... Passed 0.01 sec\n' for n in range(1,401))+'100% tests passed, 0 tests failed out of 400\n','vitest':''.join(f' ✓ tests/test{n}.test.ts (1 test) 1ms\n' for n in range(400))+' Test Files  400 passed (400)\n      Tests  400 passed (400)\n'}
   findings=[]
   for kind,raw in outputs.items():
    data=root/'data';data.write_text(raw);script=root/'command';script.write_text('#!/bin/sh\ncat "$1"\nprintf "STDERR_SENTINEL\\n" >&2\nexit "$2"\n');script.chmod(0o700)
    for code in [0,1,2,17,128]:
     cmd=f'set -o pipefail; {script} {data} {code} | python3 -I {component}/rtk-mode.py filter {profile} {kind}'
     r=subprocess.run(['bash','-c',cmd],capture_output=True,env=env,cwd=root)
     self.assertEqual(r.returncode,code,(kind,r));self.assertEqual(r.stderr,b'STDERR_SENTINEL\n',kind)
     if b'RTK full output:' in r.stdout:
      path=r.stdout.decode().split('[RTK full output: ')[1].split(']')[0];self.assertEqual(Path(path).read_bytes(),raw.encode())
     if kind=='build-preview':self.assertIn(b'BUILD SUCCESSFUL in 2s',r.stdout)
    findings.append({'filter':kind,'input':len(raw.encode()),'output':len(r.stdout),'exitCodesPreserved':[0,1,2,17,128]})
    data.write_text(raw+'\nFAILED: critical assertion details\n')
    r=subprocess.run(['python3','-I',str(component/'rtk-mode.py'),'filter',profile,kind],input=data.read_bytes(),capture_output=True,env=env);self.assertEqual(r.stdout,data.read_bytes(),kind)
   (REPO/'.verification/rtk-expand/filter-results.json').write_text(json.dumps(findings,indent=2))
 def test_atomic_helper_refresh_preserves_state(self):
  with tempfile.TemporaryDirectory(dir=REPO/'.verification/rtk-expand') as tmp:
   old=m.BASE;m.BASE=Path(tmp)
   try:
    d=m.directory(m.component());(d/'rtk-mode.py').write_text('old');(d/'rtk').write_bytes(b'unchanged')
    m.refresh_helper('new bridge');self.assertEqual((d/'rtk-mode.py').read_text(),'new bridge');self.assertEqual((d/'rtk').read_bytes(),b'unchanged')
   finally:m.BASE=old
if __name__=='__main__':unittest.main()
