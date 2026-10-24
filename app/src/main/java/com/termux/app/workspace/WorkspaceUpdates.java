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
    /** One attempt per foreground opening, including failed/disabled/busy openings. */
    static final class Opening {
        private boolean entered;
        boolean enter(boolean enabled){if(entered)return false;entered=true;return enabled;}
        void background(boolean changingConfiguration){if(!changingConfiguration)entered=false;}
    }
    private static final Opening OPENING=new Opening();
    static void background(Activity a){OPENING.background(a.isChangingConfigurations());}
    private static android.content.SharedPreferences prefs(Activity a){return a.getSharedPreferences("workspace-updates",0);}
    static boolean autoEnabled(Activity a){return prefs(a).getBoolean("automatic",true);}
    static void autoEnabled(Activity a,boolean enabled){prefs(a).edit().putBoolean("automatic",enabled).apply();}
    static boolean available(Activity a){return prefs(a).getLong("availableCode",0)>com.termux.BuildConfig.VERSION_CODE;}
    static String menuLabel(Activity a){return label(a)+(available(a)?" · "+prefs(a).getString("availableName",""):"");}
    static android.widget.LinearLayout menuRow(Activity a,Runnable manual){
        android.widget.LinearLayout row=new android.widget.LinearLayout(a);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackground(WorkspaceDialogs.background(a,false));
        android.widget.TextView title=WorkspaceDialogs.action(a,menuLabel(a),0);title.setBackground(null);
        android.widget.LinearLayout labels=new android.widget.LinearLayout(a);labels.setOrientation(android.widget.LinearLayout.VERTICAL);
        labels.addView(title);
        android.widget.TextView hint=new android.widget.TextView(a);hint.setText(text(a,"Проверять при открытии приложения","Check when opening the app"));hint.setTextColor(WorkspaceDialogs.MUTED);hint.setTextSize(12);
        int pad=WorkspaceDialogs.dp(a,16);hint.setPadding(pad,0,0,WorkspaceDialogs.dp(a,12));labels.addView(hint);
        row.addView(labels,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.CheckBox automatic=new android.widget.CheckBox(a);automatic.setChecked(autoEnabled(a));
        automatic.setButtonTintList(android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT));
        automatic.setContentDescription(text(a,"Автопроверка при открытии приложения","Check automatically when opening the app"));
        automatic.setText(text(a,"Авто","Auto"));automatic.setTextSize(12);automatic.setTextColor(WorkspaceDialogs.MUTED);
        int size=WorkspaceDialogs.dp(a,54);row.addView(automatic,new android.widget.LinearLayout.LayoutParams(WorkspaceDialogs.dp(a,90),size));
        automatic.setOnCheckedChangeListener((button,checked)->autoEnabled(a,checked));
        title.setOnClickListener(v->manual.run());labels.setOnClickListener(v->manual.run());row.setOnClickListener(v->manual.run());return row;
    }
    private static void remember(Activity a,Release r){prefs(a).edit().putLong("availableCode",r.code).putString("availableName",r.name).apply();}
    static void automatic(Activity a,Runnable changed){
        if(!OPENING.enter(autoEnabled(a))||!BUSY.compareAndSet(false,true))return;
        new Thread(()->{try{Release release=latest(a,new UpdateConnection());if(release!=null)remember(a,release);}catch(Exception ignored){}finally{BUSY.set(false);a.runOnUiThread(()->{if(alive(a))changed.run();});}},"jobs-update-auto").start();
    }
    private static Release latest(Activity a,UpdateConnection cancelled)throws Exception{
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
    private static byte[] fetch(String url,long limit,UpdateConnection cancelled)throws Exception{
        HttpURLConnection c=cancelled.connect(url);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;cancelled.check();while((n=in.read(b))!=-1){if(cancelled.isCancelled())throw new InterruptedIOException();if(out.size()+n>limit)throw new IOException("Response too large");out.write(b,0,n);}cancelled.check();return out.toByteArray();
        }finally{cancelled.release(c);}
    }
    static void check(Activity a){
        if(!BUSY.compareAndSet(false,true)){message(a,text(a,"Проверка или скачивание уже выполняется.","A check or download is already running."));return;}
        UpdateConnection cancelled=new UpdateConnection();UpdateProgress indicator=new UpdateProgress(a);AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(label(a)).setView(indicator.view).setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->cancelled.cancel()).create();wait.setOnCancelListener(d->cancelled.cancel());wait.show();
        new Thread(()->{Runnable[] result={null};try{
            Release r=latest(a,cancelled);
            if(r==null){result[0]=()->message(a,text(a,"Последний релиз пока не содержит данных для встроенного обновления. Установлено: ","The latest release does not yet support in-app updates. Installed: ")+com.termux.BuildConfig.VERSION_NAME);return;}
            remember(a,r);
            PackageInfo installed=a.getPackageManager().getPackageInfo(a.getPackageName(),0);
            result[0]=()->{if(cancelled.isCancelled()||!alive(a))return;
                if(r.code<=version(installed)){message(a,text(a,"Установлена актуальная версия: ","You are up to date: ")+installed.versionName);return;}
                if(Build.VERSION.SDK_INT<r.minSdk||!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")){message(a,text(a,"Обновление несовместимо с этим устройством.","This update is not compatible with this device."));return;}
                WorkspaceDialogs.builder(a).setTitle(text(a,"Обновление ","Update ")+r.name).setMessage(r.notes+"\n"+text(a,"Данные сохраняются при обновлении поверх. Не удаляйте приложение. Завершите важные задачи: Android может остановить процессы приложения при установке.","An in-place update keeps your data. Do not uninstall the app. Finish important tasks: Android may stop app processes during installation."))
                    .setPositiveButton(text(a,"Скачать обновление","Download update"),(d,w)->download(a,r)).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();};
        }catch(Exception e){if(!cancelled.isCancelled())result[0]=()->message(a,text(a,"Не удалось проверить обновления. Проверьте интернет и повторите позже.\n","Could not check for updates. Check your connection and try later.\n")+safeError(e));}
        finally{BUSY.set(false);a.runOnUiThread(()->{if(alive(a)){wait.dismiss();if(!cancelled.isCancelled()&&result[0]!=null)result[0].run();}});}},"jobs-update-check").start();
    }
    private static String safeError(Exception e){return e instanceof IOException&&e.getMessage()!=null&&e.getMessage().matches("GitHub HTTP [0-9]{3}")?e.getMessage():e.getClass().getSimpleName();}
    /** UI state only. Download authenticity and cancellation remain in their existing worker. */
    static final class UpdateProgress {
        final android.widget.LinearLayout view;
        final android.widget.ProgressBar bar;
        final android.widget.TextView status;
        private final Activity activity;
        UpdateProgress(Activity a){
            activity=a;view=new android.widget.LinearLayout(a);view.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad=WorkspaceDialogs.dp(a,20);view.setPadding(pad,pad,pad,pad);
            status=new android.widget.TextView(a);status.setTextColor(WorkspaceDialogs.TEXT);status.setTextSize(14);view.addView(status);
            bar=new android.widget.ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);
            android.content.res.ColorStateList tint=android.content.res.ColorStateList.valueOf(WorkspaceDialogs.ACCENT);
            bar.setProgressTintList(tint);bar.setIndeterminateTintList(tint);
            android.widget.LinearLayout.LayoutParams params=new android.widget.LinearLayout.LayoutParams(-1,WorkspaceDialogs.dp(a,6));params.topMargin=WorkspaceDialogs.dp(a,16);view.addView(bar,params);
            checking();
        }
        void checking(){bar.setIndeterminate(true);status.setText(text(activity,"Проверка GitHub…","Checking GitHub…"));}
        void downloaded(long bytes,long total){
            if(total<=0){bar.setIndeterminate(true);status.setText(text(activity,"Подключение к загрузке…","Connecting to download…"));return;}
            long count=Math.max(0,Math.min(bytes,total));int percent=(int)(count*100/total);
            bar.setIndeterminate(false);bar.setProgress(percent);
            status.setText(percent+"% · "+android.text.format.Formatter.formatFileSize(activity,count)+" / "+android.text.format.Formatter.formatFileSize(activity,total));
        }
        void verifying(){bar.setIndeterminate(true);status.setText(text(activity,"Загрузка завершена. Проверка APK и подписи…","Download complete. Verifying APK and signature…"));}
    }
    private static void download(Activity a,Release r){
        if(!BUSY.compareAndSet(false,true))return;
        UpdateConnection cancelled=new UpdateConnection();UpdateProgress indicator=new UpdateProgress(a);indicator.downloaded(0,r.size);AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(text(a,"Скачивание обновления","Downloading update")).setView(indicator.view)
            .setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->cancelled.cancel()).create();wait.setOnCancelListener(d->cancelled.cancel());wait.show();
        new Thread(()->{File output=null;boolean ready=false;try{
            File dir=new File(a.getCacheDir(),"workspace-updates");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cache unavailable");
            // Only updater-owned files; never touch user storage or shell environments.
            File[] old=dir.listFiles();if(old!=null)for(File f:old)if(f.getName().startsWith("update-")&&f.isFile())f.delete();
            output=File.createTempFile("update-",".apk",dir);MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;int shown=-1;
            HttpURLConnection c=cancelled.connect(r.url);try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(output)){
                byte[] b=new byte[65536];int n;cancelled.check();while((n=in.read(b))!=-1){if(cancelled.isCancelled())throw new InterruptedIOException();count+=n;if(count>r.size)throw new IOException("APK size mismatch");out.write(b,0,n);hash.update(b,0,n);int percent=(int)(count*100/r.size);if(percent!=shown){shown=percent;final long downloaded=count;a.runOnUiThread(()->{if(alive(a)&&wait.isShowing())indicator.downloaded(downloaded,r.size);});}}
            }finally{cancelled.release(c);}
            if(count!=r.size||!hex(hash.digest()).equals(r.hash))throw new IOException("APK checksum mismatch");
            a.runOnUiThread(()->{if(alive(a)&&wait.isShowing())indicator.verifying();});
            verify(a,output,r);if(cancelled.isCancelled())throw new InterruptedIOException();ready=true;File apk=output;
            a.runOnUiThread(()->{if(alive(a)&&!cancelled.isCancelled())offerInstall(a,apk,r);});
        }catch(Exception e){if(!cancelled.isCancelled())a.runOnUiThread(()->message(a,text(a,"Обновление не прошло проверку или не скачалось. Установка заблокирована; данные не изменены.\n","Download or verification failed. Installation blocked; your data was not changed.\n")+safeError(e)));}
        finally{if(!ready&&output!=null)output.delete();BUSY.set(false);a.runOnUiThread(()->{if(alive(a))wait.dismiss();});}},"jobs-update-download").start();
    }
    /** Recheck the exact cached bytes after any time spent in permission/settings dialogs. */
    static void verifyContent(File file,long size,String expectedHash)throws Exception{
        if(!file.isFile()||file.length()!=size)throw new IOException("APK size mismatch");
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;
        try(InputStream input=new FileInputStream(file)){
            byte[] buffer=new byte[65536];int n;
            while((n=input.read(buffer))!=-1){count+=n;if(count>size)throw new IOException("APK size mismatch");hash.update(buffer,0,n);}
        }
        if(count!=size||!hex(hash.digest()).equals(expectedHash))throw new IOException("APK checksum mismatch");
    }
    static void prepareInstall(Activity a,File apk,Release release){
        if(!BUSY.compareAndSet(false,true)){message(a,text(a,"Дождитесь окончания проверки или загрузки и повторите установку.","Wait for the current check or download, then try Install again."));return;}
        UpdateConnection cancelled=new UpdateConnection();UpdateProgress indicator=new UpdateProgress(a);indicator.verifying();
        AlertDialog wait=WorkspaceDialogs.builder(a).setTitle(label(a)).setView(indicator.view)
            .setNegativeButton(WorkspaceText.tr("Отмена"),(d,w)->cancelled.cancel()).create();
        wait.setOnCancelListener(d->cancelled.cancel());wait.show();
        new Thread(()->{
            Exception failure=null;
            try{verifyContent(apk,release.size,release.hash);cancelled.check();verify(a,apk,release);cancelled.check();}
            catch(Exception e){failure=e;}
            finally{
                Exception error=failure;
                a.runOnUiThread(()->{
                    BUSY.set(false);
                    if(!alive(a))return;
                    wait.dismiss();if(cancelled.isCancelled())return;
                    if(error!=null){message(a,text(a,"Сохранённый APK недоступен или не прошёл повторную проверку. Скачайте обновление заново.","The cached APK is unavailable or failed verification. Download the update again."));return;}
                    try{
                        Uri uri=FileProvider.getUriForFile(a,a.getPackageName()+".workspace.files",apk);
                        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setClipData(android.content.ClipData.newRawUri("Update APK",uri));a.startActivity(intent);
                    }catch(Exception e){message(a,text(a,"Не удалось открыть установщик. Повторите загрузку обновления.","Could not open the installer. Download the update again."));}
                });
            }
        },"jobs-update-install-check").start();
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
                    prepareInstall(a,apk,r);
                }catch(Exception e){message(a,text(a,"Не удалось открыть установщик. Приложение не удалялось.","Could not open the installer. The app was not uninstalled."));}
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
}
