from pathlib import Path
import os,subprocess,json
root=Path.cwd();out=root/'.verification/release-reproduction-v2.1.7';src=out/'source';stage=src/'.verification/arm64'
assert not (src/'.signing').exists() and not (stage/'.signing').exists()
for n in ['gradle-home','java-home','tmp']:(out/n).mkdir(exist_ok=True)
# Whitelist tool environment; no GitHub/account/signing variables enter Gradle.
env={k:v for k,v in os.environ.items() if k in ['PATH','LD_LIBRARY_PATH','LANG','LC_ALL','TZ','PREFIX']}
env.update(JAVA_HOME=str(root/'.tools/jdk/usr/lib/jvm/java-11-openjdk-arm64'),GRADLE_USER_HOME=str(out/'gradle-home'),GRADLE_RO_DEP_CACHE=str(root/'.tools/gradle-home/caches'),ANDROID_HOME=str(src/'.tools/sdk'),ANDROID_SDK_ROOT=str(src/'.tools/sdk'),TMPDIR=str(out/'tmp'),TERMUX_APP_VERSION_NAME='2.1.7',TERMUX_APK_VERSION_TAG='v2.1.7-arm64',TERMUX_SPLIT_APKS_FOR_RELEASE_BUILDS='0',TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS='0',REPRO_EVIDENCE=str(out))
cmd=[str(root/'.tools/gradle-7.2/bin/gradle'),'--offline','--no-daemon','--no-build-cache','--rerun-tasks','--max-workers=2','-Dorg.gradle.jvmargs=-Xmx1536m -Duser.home='+str(out/'java-home'),'-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2','-Pandroid.builder.sdkDownload=false','-I',str(out/'unsigned.init.gradle'),'-x',':app:downloadBootstraps',':app:assembleReproduction']
(out/'gradle-command.json').write_text(json.dumps(cmd,indent=2))
with (out/'build.log').open('w') as f:subprocess.run(cmd,cwd=stage,env=env,stdout=f,stderr=subprocess.STDOUT,check=True)
print('Unsigned reproduction complete')
