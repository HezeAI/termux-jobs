package com.termux.app.workspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

/** Manual public-release updates. Never reads shell credentials or changes user data. */
final class WorkspaceUpdates {
    static final String API="https://api.github.com/repos/HezeAI/termux-jobs/releases/latest";
    static final String DOWNLOAD="https://github.com/HezeAI/termux-jobs/releases/download/";
    private static final long MAX_APK=256L*1024*1024;
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private WorkspaceUpdates() {}
    static String label(Activity a){return text(a,"Проверить обновления","Check for updates");}
    private static String text(Activity a,String ru,String en){return WorkspaceLanguage.code(a).equals("ru")?ru:en;}
    private static boolean alive(Activity a){return !a.isFinishing()&&!a.isDestroyed();}
    private static void message(Activity a,String value){if(alive(a))WorkspaceDialogs.builder(a).setTitle(label(a)).setMessage(value).setPositiveButton("OK",null).show();}
    static final long CHECK_INTERVAL=10L*60*60*1000;
    private static android.content.SharedPreferences prefs(Activity a){return a.getSharedPreferences("workspace-updates",0);}
    static boolean autoEnabled(Activity a){return prefs(a).getBoolean("automatic",true);}
    static void autoEnabled(Activity a,boolean enabled){prefs(a).edit().putBoolean("automatic",enabled).apply();}
    static boolean due(long now,long last){return last==0||now<last||now-last>=CHECK_INTERVAL;}
    static boolean available(Activity a){return prefs(a).getLong("availableCode",0)>com.termux.BuildConfig.VERSION_CODE;}
    static String menuLabel(Activity a){return label(a)+(available(a)?" · "+prefs(a).getString("availableName",""):"");}
    private static void remember(Activity a,Release r){prefs(a).edit().putLong("availableCode",r.code).putString("availableName",r.name).apply();}
    static void automatic(Activity a,Runnable changed){
        long now=System.currentTimeMillis();if(!autoEnabled(a)||!due(now,prefs(a).getLong("attempt",0))||!BUSY.compareAndSet(false,true))return;
        prefs(a).edit().putLong("attempt",now).apply();
        new Thread(()->{try{Release release=latest(a,new AtomicBoolean());if(release!=null)remember(a,release);}catch(Exception ignored){}finally{BUSY.set(false);a.runOnUiThread(()->{if(alive(a))changed.run();});}},"jobs-update-auto").start();
    }
    private static Release latest(Activity a,AtomicBoolean cancelled)throws Exception{
        JSONObject latest=new JSONObject(new String(fetch(API,1024*1024,cancelled),"UTF-8"));
        if(latest.optBoolean("draft")||latest.optBoolean("prerelease"))throw new IOException("Not a stable release");
        String tag=latest.getString("tag_name");JSONArray assets=latest.getJSONArray("assets");
        for(int i=0;i<assets.length();i++){JSONObject asset=assets.getJSONObject(i);if(asset.getString("name").equals("update.json")){
            String url=asset.getString("browser_download_url");if(!validAsset(url,tag,"update.json"))throw new IOException("Invalid manifest URL");
            return new Release(new JSONObject(new String(fetch(url,64*1024,cancelled),"UTF-8")),tag,WorkspaceLanguage.code(a).equals("ru"));
        }}return null;
    }
    static long version(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    static final class Release {
        final long code,size;final String name,url,hash,notes;final int minSdk;
        Release(JSONObject m,String tag,boolean ru)throws Exception{
            if(m.getInt("schema")!=1)throw new IOException("Unsupported update manifest");
            code=m.getLong("versionCode");name=m.getString("versionName");minSdk=m.getInt("minSdk");
            JSONObject apk=m.getJSONObject("apk");url=apk.getString("url");hash=apk.getString("sha256");size=apk.getLong("size");
            if(code<=0||!name.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!tag.equals("v"+name)||!hash.matches("[a-f0-9]{64}")||size<=0||size>MAX_APK||minSdk<24||!m.getString("packageId").equals("com.termux")||!apk.getString("abi").equals("arm64-v8a")||!validAsset(url,tag,"Termux-Jobs-v"+name+"-arm64.apk"))throw new IOException("Invalid update manifest");
            JSONArray lines=m.getJSONObject("changes").getJSONArray(ru?"ru":"en");
            if(lines.length()==0||lines.length()>20)throw new IOException("Invalid release notes");
            StringBuilder b=new StringBuilder();for(int i=0;i<lines.length();i++){String s=lines.getString(i);if(s.length()>1000)throw new IOException("Release note too long");b.append("• ").append(s).append('\n');}notes=b.toString();
        }
    }
    static boolean validAsset(String url,String tag,String name){return tag.matches("v[0-9]+\\.[0-9]+\\.[0-9]+")&&url.equals(DOWNLOAD+tag+"/"+name);}
    private static HttpURLConnection connect(String address)throws Exception{
        URL url=new URL(address);
        // GitHub assets redirect to release-assets.githubusercontent.com; never permit HTTP.
        for(int n=0;n<6;n++){
            String host=url.getHost();if(!url.getProtocol().equals("https")||url.getUserInfo()!=null||url.getPort()!=-1||!(host.equals("api.github.com")||host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")))throw new IOException("Untrusted download host");
            HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Termux-Jobs-Updater");c.setRequestProperty("Accept",host.equals("api.github.com")?"application/vnd.github+json":"application/octet-stream");
            int status=c.getResponseCode();if(status>=300&&status<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Missing redirect");url=new URL(url,location);continue;}
            if(status!=200){c.disconnect();throw new IOException("GitHub HTTP "+status);}return c;
        }throw new IOException("Too many redirects");
    }
    private static byte[] fetch(String url,long limit,AtomicBoolean cancelled)throws Exception{
        HttpURLConnection c=connect(url);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(cancelled.get())throw new InterruptedIOException();if(out.size()+n>limit)throw new IOException("Response too large");out.write(b,0,n);}return out.toByteArray();
        }finally{c.disconnect();}
    }
    static void check(Activity a){
        if(!BUSY.compareAndSet(false,true)){message(a,text(a,"Проверка или скачивание уже выполняется.","A check or download is already running."));return;}
        prefs(a).edit().putLong("attempt",System.currentTimeMillis()).apply();
        AtomicBoolean cancelled=new AtomicBoolean();AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(label(a)).setMessage(text(a,"Проверка GitHub…","Checking GitHub…")).setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->cancelled.set(true)).create();wait.setOnCancelListener(d->cancelled.set(true));wait.show();
        new Thread(()->{Runnable[] result={null};try{
            Release r=latest(a,cancelled);
            if(r==null){result[0]=()->message(a,text(a,"Последний релиз пока не содержит данных для встроенного обновления. Установлено: ","The latest release does not yet support in-app updates. Installed: ")+com.termux.BuildConfig.VERSION_NAME);return;}
            remember(a,r);
            PackageInfo installed=a.getPackageManager().getPackageInfo(a.getPackageName(),0);
            result[0]=()->{if(cancelled.get()||!alive(a))return;
                if(r.code<=version(installed)){message(a,text(a,"Установлена актуальная версия: ","You are up to date: ")+installed.versionName);return;}
                if(Build.VERSION.SDK_INT<r.minSdk||!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")){message(a,text(a,"Обновление несовместимо с этим устройством.","This update is not compatible with this device."));return;}
                WorkspaceDialogs.builder(a).setTitle(text(a,"Обновление ","Update ")+r.name).setMessage(r.notes+"\n"+text(a,"Данные сохраняются при обновлении поверх. Не удаляйте приложение. Завершите важные задачи: Android может остановить процессы приложения при установке.","An in-place update keeps your data. Do not uninstall the app. Finish important tasks: Android may stop app processes during installation."))
                    .setPositiveButton(text(a,"Скачать обновление","Download update"),(d,w)->download(a,r)).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();};
        }catch(Exception e){if(!cancelled.get())result[0]=()->message(a,text(a,"Не удалось проверить обновления. Проверьте интернет и повторите позже.\n","Could not check for updates. Check your connection and try later.\n")+safeError(e));}
        finally{BUSY.set(false);a.runOnUiThread(()->{if(alive(a)){wait.dismiss();if(!cancelled.get()&&result[0]!=null)result[0].run();}});}},"jobs-update-check").start();
    }
    private static String safeError(Exception e){return e instanceof IOException&&e.getMessage()!=null&&e.getMessage().matches("GitHub HTTP [0-9]{3}")?e.getMessage():e.getClass().getSimpleName();}
    private static void download(Activity a,Release r){
        if(!BUSY.compareAndSet(false,true))return;
        AtomicBoolean cancelled=new AtomicBoolean();AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(text(a,"Скачивание обновления","Downloading update")).setMessage("0%")
            .setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->cancelled.set(true)).create();wait.setOnCancelListener(d->cancelled.set(true));wait.show();
        new Thread(()->{File output=null;boolean ready=false;try{
            File dir=new File(a.getCacheDir(),"workspace-updates");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cache unavailable");
            // Only updater-owned files; never touch user storage or shell environments.
            File[] old=dir.listFiles();if(old!=null)for(File f:old)if(f.getName().startsWith("update-")&&f.isFile())f.delete();
            output=File.createTempFile("update-",".apk",dir);MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;int shown=-1;
            HttpURLConnection c=connect(r.url);try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(output)){
                byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(cancelled.get())throw new InterruptedIOException();count+=n;if(count>r.size)throw new IOException("APK size mismatch");out.write(b,0,n);hash.update(b,0,n);int percent=(int)(count*100/r.size);if(percent!=shown){shown=percent;final int progress=percent;a.runOnUiThread(()->{if(alive(a)&&wait.isShowing())wait.setMessage(progress+"%");});}}
            }finally{c.disconnect();}
            if(count!=r.size||!hex(hash.digest()).equals(r.hash))throw new IOException("APK checksum mismatch");
            verify(a,output,r);if(cancelled.get())throw new InterruptedIOException();ready=true;File apk=output;
            a.runOnUiThread(()->{if(alive(a)&&!cancelled.get())offerInstall(a,apk,r);});
        }catch(Exception e){if(!cancelled.get())a.runOnUiThread(()->message(a,text(a,"Обновление не прошло проверку или не скачалось. Установка заблокирована; данные не изменены.\n","Download or verification failed. Installation blocked; your data was not changed.\n")+safeError(e)));}
        finally{if(!ready&&output!=null)output.delete();BUSY.set(false);a.runOnUiThread(()->{if(alive(a))wait.dismiss();});}},"jobs-update-download").start();
    }
    @SuppressWarnings("deprecation")
    static void verify(Activity a,File file,Release r)throws Exception{
        try(ZipFile zip=new ZipFile(file)){if(zip.getEntry("lib/arm64-v8a/libtermux.so")==null)throw new IOException("Missing ARM64 library");}
        PackageManager pm=a.getPackageManager();PackageInfo apk=pm.getPackageArchiveInfo(file.getPath(),PackageManager.GET_SIGNATURES),installed=pm.getPackageInfo(a.getPackageName(),PackageManager.GET_SIGNATURES);
        validatePackage(a.getPackageName(),installed,apk,r,Build.VERSION.SDK_INT);
    }
    static void validatePackage(String packageName,PackageInfo installed,PackageInfo apk,Release r,int sdk)throws IOException{
        if(apk==null||!packageName.equals(apk.packageName)||version(apk)!=r.code||version(apk)<=version(installed)||!r.name.equals(apk.versionName)||apk.applicationInfo==null||apk.applicationInfo.minSdkVersion>sdk||!sameSignatures(installed.signatures,apk.signatures))throw new IOException("APK package, version or signature mismatch");
    }
    static boolean sameSignatures(Signature[] a,Signature[] b){if(a==null||b==null||a.length==0||a.length!=b.length)return false;Set<Signature> x=new HashSet<>(Arrays.asList(a)),y=new HashSet<>(Arrays.asList(b));return x.size()==a.length&&x.equals(y);}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return s.toString();}
    private static void offerInstall(Activity a,File apk,Release r){
        WorkspaceDialogs.builder(a).setTitle(text(a,"Обновление проверено","Update verified")).setMessage(text(a,"Пакет, версия, подпись и SHA-256 проверены. Android запросит подтверждение установки. Данные сохраняются при обновлении поверх.","Package, version, signature and SHA-256 verified. Android will ask you to confirm installation. An in-place update keeps your data."))
            .setPositiveButton(text(a,"Установить","Install"),(d,w)->{
                try{
                    if(Build.VERSION.SDK_INT>=26&&!a.getPackageManager().canRequestPackageInstalls()){
                        WorkspaceDialogs.builder(a).setTitle(text(a,"Разрешение на установку","Installation permission")).setMessage(text(a,"Разрешите установку из этого приложения в настройках Android. После возвращения нажмите «Продолжить».","Allow installation from this app in Android settings. When you return, tap Continue."))
                            .setNeutralButton(text(a,"Настройки Android","Android settings"),(p,q)->{try{a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));offerInstall(a,apk,r);}catch(Exception e){message(a,text(a,"Не удалось открыть настройки Android.","Could not open Android settings."));}})
                            .setPositiveButton(text(a,"Продолжить","Continue"),(p,q)->offerInstall(a,apk,r)).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();return;
                    }
                    verify(a,apk,r);Uri uri=FileProvider.getUriForFile(a,a.getPackageName()+".workspace.files",apk);
                    Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(intent);
                }catch(Exception e){message(a,text(a,"Не удалось открыть установщик. Приложение не удалялось.","Could not open the installer. The app was not uninstalled."));}
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
}
