#!/usr/bin/env python3
"""Check the production certificate gate using synthetic keystores only."""
from pathlib import Path
import subprocess,tempfile,os,json
ROOT=Path(__file__).resolve().parents[2];BASE=ROOT/'.verification/release-signer-check';BASE.mkdir(exist_ok=True,parents=True)
work=Path(tempfile.mkdtemp(prefix='fixture-',dir=BASE));store=work/'fixture.jks';der=work/'public.der'
env=dict(os.environ,FIXTURE_PASSWORD='synthetic-fixture-password-not-a-real-secret')
subprocess.run(['keytool','-genkeypair','-alias','fixture','-keyalg','RSA','-keysize','2048','-storetype','JKS','-keystore',str(store),'-storepass:env','FIXTURE_PASSWORD','-keypass:env','FIXTURE_PASSWORD','-dname','CN=Synthetic Fixture','-validity','1','-noprompt'],env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
subprocess.run(['keytool','-exportcert','-alias','fixture','-keystore',str(store),'-storepass:env','FIXTURE_PASSWORD','-file',str(der)],env=env,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
import hashlib
pin=work/'expected.sha256';pin.write_text(hashlib.sha256(der.read_bytes()).hexdigest()+'\n');wrong=work/'wrong.sha256';wrong.write_bytes((ROOT/'gradle/release-certificate.sha256').read_bytes())
(work/'settings.gradle').write_text("rootProject.name='synthetic-signer-check'\n")
script='''apply from: ROOT_SCRIPT
import groovy.json.JsonOutput
task probe {
 doLast {
  def results=[]
  def check={String name,File store,String alias,String password,File pin,String expected ->
   String actual='PASS'
   try{project.ext.verifyPinnedReleaseSigner(store,alias,password.toCharArray(),pin)}catch(GradleException e){actual=e.message}
   assert actual==expected: name+': '+actual
   results.add([name:name,result:actual])
  }
  def store=file('fixture.jks'),pin=file('expected.sha256'),secret=System.getenv('FIXTURE_PASSWORD')
  check('missing key',file('missing.jks'),'fixture',secret,pin,'RELEASE_KEY_MISSING')
  check('wrong certificate',store,'fixture',secret,file('wrong.sha256'),'RELEASE_SIGNER_MISMATCH')
  check('missing pin',store,'fixture',secret,file('missing-pin'),'RELEASE_CERTIFICATE_PIN_MISSING')
  check('wrong password',store,'fixture','deliberately-wrong',pin,'RELEASE_CERTIFICATE_UNAVAILABLE')
  check('missing alias',store,'missing-alias',secret,pin,'RELEASE_CERTIFICATE_UNAVAILABLE')
  check('correct existing synthetic key',store,'fixture',secret,pin,'PASS')
  check('mismatch on next attempt',store,'fixture',secret,file('wrong.sha256'),'RELEASE_SIGNER_MISMATCH')
  check('retry correct existing key',store,'fixture',secret,pin,'PASS')
  file('results.json').text=JsonOutput.prettyPrint(JsonOutput.toJson(results))
 }
}
'''.replace('ROOT_SCRIPT',json.dumps(str(ROOT/'gradle/verify-release-signer.gradle')))
(work/'build.gradle').write_text(script)
env.update(GRADLE_USER_HOME=str(ROOT/'.tools/gradle-home'),JAVA_HOME=str(ROOT/'.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64'),JAVA_TOOL_OPTIONS='-Djava.io.tmpdir='+str(work))
r=subprocess.run([str(ROOT/'.tools/gradle-7.2/bin/gradle'),'--offline','--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx512m','probe'],cwd=work,env=env,capture_output=True,text=True)
(BASE/'synthetic-gradle.log').write_text(r.stdout+r.stderr);assert r.returncode==0,r.stdout+r.stderr
rows=json.loads((work/'results.json').read_text());(BASE/'results.json').write_text(json.dumps({'realKeyAccessed':False,'tests':rows},indent=2)+'\n');print(json.dumps(rows,indent=2))
