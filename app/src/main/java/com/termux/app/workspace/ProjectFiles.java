package com.termux.app.workspace;

import android.app.Activity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded, independent file operations. Never types into the user's terminal. */
final class ProjectFiles implements AutoCloseable {
    static final int MAX_LIST = 2 * 1024 * 1024, MAX_PREVIEW = 256 * 1024;
    static final long MAX_EXPORT = 256L * 1024 * 1024;
    final Activity activity; final WorkspaceStore.Session profile;
    private final ProjectFileTransport transport=new ProjectFileTransport();
    private volatile boolean closed;
    private char[] password;
    private File serverKnownHosts;
    void setServerKnownHosts(File file){serverKnownHosts=file;}
    ProjectFiles(Activity activity, WorkspaceStore.Session profile) {this.activity=activity;this.profile=profile.copy();this.profile.id=profile.id;
        this.profile.name=profile.name;this.profile.handle=profile.handle;this.profile.codexSocketId=profile.codexSocketId;
        this.profile.sshEncryptedPassword=profile.sshEncryptedPassword;}
    synchronized void setPassword(char[] value) {if(closed)return;if(password!=null)Arrays.fill(password,'\0');password=value.clone();}
    private synchronized char[] passwordCopy(){return password==null?null:password.clone();}
    static String environment(WorkspaceStore.Session s) {return !AgentPreparation.agent(s).isEmpty()&&s.codexEnvironment.equals("ubuntu-ai")?"proot:ubuntu-ai":!s.prootAlias.isEmpty()?"proot:"+s.prootAlias:s.environment.equals("codex")?s.codexEnvironment:s.environment.equals("legacy")?"termux":s.environment;}
    static String environmentLabel(WorkspaceStore.Session s) {String e=environment(s);return e.startsWith("proot:")?WorkspaceEnvironments.label(e.substring(6)):e.equals("ubuntu")?"Ubuntu":e.equals("ssh")?"SSH · "+s.sshHost:"Termux";}
    static String target(String path) {
        if(path==null || path.indexOf('\0')>=0 || path.contains("\n") || path.contains("\r"))throw new IllegalArgumentException(WorkspaceText.tr("Некорректный путь"));
        if(path.isEmpty() || path.equals("~"))return "\"$HOME\"";
        return path.startsWith("~/")?"\"$HOME\"/"+SessionShell.quote(path.substring(2)):SessionShell.quote(path);
    }
    static String listScript(String path) {
        return "cd -- "+target(path)+" || exit 2; printf 'JOBS_FILES_2\\0%s\\0' \"$(pwd -P)\"; exec find -L . -mindepth 1 -maxdepth 1 -printf '%y\\0%f\\0%s\\0%T@\\0'";
    }
    static final class Entry {
        final String name,path;final boolean directory;final long size,modified;
        Entry(String parent,String name,boolean directory,long size) {this(parent,name,directory,size,-1);}
        Entry(String parent,String name,boolean directory,long size,long modified) {this.modified=modified;this.name=name;this.path=(parent.equals("/")?"/":parent+"/")+name;this.directory=directory;this.size=size;}
    }
    static final class Listing {String path; final List<Entry> entries=new ArrayList<>();}
    static Listing parse(byte[] bytes) throws IOException {
        String[] fields=new String(bytes,StandardCharsets.UTF_8).split("\u0000",-1);
        boolean version2=fields.length>0&&fields[0].equals("JOBS_FILES_2");int start=version2?2:1,stride=version2?4:3;
        if(fields.length<start+1 || !fields[start-1].startsWith("/") || (fields.length-start-1)%stride!=0)throw new IOException(WorkspaceText.tr("Некорректный список файлов"));
        Listing result=new Listing();result.path=fields[start-1];
        for(int i=start;i+stride-1<fields.length;i+=stride) {
            if(fields[i+1].isEmpty() || fields[i+1].contains("/") || fields[i+1].equals(".") || fields[i+1].equals(".."))throw new IOException(WorkspaceText.tr("Некорректное имя файла"));
            long size;try {size=Long.parseLong(fields[i+2]);}catch(NumberFormatException e){throw new IOException(WorkspaceText.tr("Некорректный размер файла"));}
            long modified=-1;if(version2){String value=fields[i+3];if(!value.matches("-?[0-9]{1,13}(\\.[0-9]{1,12})?"))throw new IOException("INVALID_FILE_TIME");double seconds=Double.parseDouble(value);if(!Double.isFinite(seconds)||Math.abs(seconds)>9e12)throw new IOException("INVALID_FILE_TIME");modified=(long)(seconds*1000);}
            if(fields[i].equals("d") || fields[i].equals("f"))result.entries.add(new Entry(result.path,fields[i+1],fields[i].equals("d"),size,modified));
            if(result.entries.size()>4000)throw new IOException(WorkspaceText.tr("Слишком много файлов; выберите вложенную папку"));
        }
        result.entries.sort(Comparator.comparing((Entry e)->!e.directory).thenComparing(e->e.name.toLowerCase(Locale.ROOT)));return result;
    }
    static String mime(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.endsWith(".apk"))return "application/vnd.android.package-archive";
        if(n.endsWith(".png"))return "image/png";if(n.endsWith(".jpg")||n.endsWith(".jpeg"))return "image/jpeg";
        if(n.endsWith(".webp"))return "image/webp";if(n.endsWith(".gif"))return "image/gif";
        if(n.endsWith(".mp4")||n.endsWith(".m4v"))return "video/mp4";if(n.endsWith(".webm"))return "video/webm";if(n.endsWith(".mkv"))return "video/x-matroska";if(n.endsWith(".mov"))return "video/quicktime";if(n.endsWith(".3gp"))return "video/3gpp";
        String ext=n.lastIndexOf('.')<0?"":n.substring(n.lastIndexOf('.')+1);
        if(Arrays.asList("md","txt","log","json","xml","yml","yaml","java","kt","kts","gradle","sh","py","js","ts","css","html","rs","toml","properties","c","h","cpp","gitignore").contains(ext)||n.equals("makefile")||n.equals("dockerfile"))return "text/plain";
        return "application/octet-stream";
    }
    static boolean visible(Entry e,String filter) {return e.directory || filter.equals(WorkspaceText.tr("Все")) || filter.equals(WorkspaceText.tr("Текст"))&&mime(e.name).equals("text/plain") || filter.equals(WorkspaceText.tr("Изображения"))&&mime(e.name).startsWith("image/") || filter.equals("APK")&&e.name.toLowerCase(Locale.ROOT).endsWith(".apk");}
    static String portableListScript(String path){return "cd -- "+target(path)+" || exit 2; printf 'JOBS_FILES_2\\0%s\\0' \"$(pwd -P)\"; for p in ./* ./.[!.]* ./..?*; do if [ -d \"$p\" ]; then kind=d; metadata=$(stat -L -c '%Y' -- \"$p\") || exit 2; size=0; modified=$metadata; elif [ -f \"$p\" ]; then kind=f; metadata=$(stat -L -c '%s %Y' -- \"$p\") || exit 2; size=${metadata%% *}; modified=${metadata##* }; else continue; fi; printf '%s\\0%s\\0%s\\0%s\\0' \"$kind\" \"${p#./}\" \"$size\" \"$modified\"; done";}
    Listing list(String path) throws Exception {return parse(bytes(environment(profile).startsWith("proot:")?portableListScript(path):listScript(path),MAX_LIST));}
    Listing createDirectory(String parent,String name) throws Exception {
        if(name.trim().isEmpty()||name.equals(".")||name.equals("..")||name.contains("/")||name.contains("\n")||name.contains("\r")||name.indexOf('\0')>=0)throw new IOException(WorkspaceText.tr("Введите имя одной папки без / и переносов"));
        bytes("cd -- "+target(parent)+" && mkdir -- "+SessionShell.quote(name),1024);
        return list((parent.equals("/")?"/":parent+"/")+name);
    }
    byte[] preview(Entry e) throws Exception {
        if(e.size>MAX_PREVIEW)throw new IOException(WorkspaceText.tr("Текст больше 256 КБ; используйте терминал или экспорт"));
        byte[] data=bytes("test -f "+target(e.path)+" && exec cat -- "+target(e.path),MAX_PREVIEW);
        for(byte b:data)if(b==0)throw new IOException(WorkspaceText.tr("Бинарный файл; доступен экспорт"));return data;
    }
    void export(Entry e,File destination,long max) throws Exception {
        if(e.size>max)throw new IOException(WorkspaceLanguage.code(activity).equals("ru")?"Файл превышает лимит выбранной операции":"File exceeds the selected operation's size limit");
        // Chat artifacts may be several GB. Keep bounded streaming and owned
        // child cancellation, without the small-query 90-second deadline.
        int timeout=max>MAX_EXPORT?6*60*60:90;
        try(OutputStream out=new FileOutputStream(destination)) {run("test -f "+target(e.path)+" && exec cat -- "+target(e.path),out,max,null,timeout);}
        catch(Exception error){destination.delete();throw error;}
    }
    TextFileEdits.Snapshot editRead(Entry entry) throws Exception {
        if(entry.size>TextFileEdits.MAX_BYTES)throw new IOException(WorkspaceText.tr("Редактор поддерживает текст UTF-8 до 256 КБ. Бинарные файлы и повреждённый текст не редактируются."));
        return TextFileEdits.parse(entry.path,bytes(TextFileEdits.readScript(entry.path),TextFileEdits.MAX_BYTES+513));
    }
    void editSave(TextFileEdits.Snapshot original,String value) throws Exception {
        byte[] payload=TextFileEdits.encode(value);ByteArrayOutputStream output=new ByteArrayOutputStream();
        try{run(TextFileEdits.saveScript(original),output,1024,payload);}catch(Exception error){throw new IOException(WorkspaceText.tr("Не удалось сохранить файл. Проверьте права и доступность окружения; введённый текст сохранён в окне."));}
        if(!output.toString("UTF-8").equals("SAVED"))throw new IOException(WorkspaceText.tr("Файл изменился после открытия. Сохранение отменено: закройте редактор и откройте файл заново. Ваш текст остался в окне."));
    }
    private String packageScript(String script){return environment(profile).startsWith("proot:")?EnvironmentPackages.nativeCommand(script):script;}
    EnvironmentPackages.Kind packageBackend()throws Exception{return EnvironmentPackages.kind(bytes(packageScript(EnvironmentPackages.probe()),128));}
    byte[] packageQuery(EnvironmentPackages.Kind kind,int tab)throws Exception{return bytes(packageScript(EnvironmentPackages.query(kind,tab)),PackageCatalog.MAX_BYTES);}
    byte[] packageDescription(EnvironmentPackages.Kind kind,PackageCatalog.Entry entry,boolean installed)throws Exception{return bytes(packageScript(EnvironmentPackages.description(kind,entry,installed)),256*1024);}
    byte[] packageDescription(PackageCatalog.Entry entry,boolean installed) throws Exception {
        try{return bytes(PackageCatalog.descriptionQuery(entry,installed),256*1024);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Описание недоступно. Обновите каталог или проверьте выбранное окружение."));}
    }
    byte[] packageVersions(String name) throws Exception {
        try {return bytes(environment(profile).startsWith("proot:")?EnvironmentPackages.nativeCommand("exec bash --noprofile --norc -c "+SessionShell.quote(PackageCatalog.versionsQuery(name))):PackageCatalog.versionsQuery(name),1024*1024);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Не удалось прочитать доступные версии пакета. Проверьте APT и выбранное окружение."));}
    }
    byte[] packageVersions(EnvironmentPackages.Kind kind,String name)throws Exception {
        return bytes(packageScript(EnvironmentPackageVersions.query(kind,name)),EnvironmentPackageVersions.MAX_BYTES);
    }
    byte[] packageQuery(int tab) throws Exception {
        try {return bytes(PackageCatalog.query(tab),PackageCatalog.MAX_BYTES);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Не удалось прочитать пакеты. Проверьте наличие APT/dpkg и доступность выбранного окружения."));}
    }
    static final class TransferFailure extends IOException {
        final int acknowledged;TransferFailure(int acknowledged,Throwable cause){super("TRANSFER_FAILED",cause);this.acknowledged=acknowledged;}
    }
    boolean transferReady(List<Entry> entries,String folder,boolean move,boolean rename)throws Exception {
        try{return new String(bytes(ProjectFileTransfer.script(entries,folder,move,rename,true),1024),StandardCharsets.UTF_8).equals("READY");}
        catch(Exception error){return false;}
    }
    void transfer(List<Entry> entries,String folder,boolean move,boolean rename)throws Exception {
        ProjectFileTransfer.execute(entries,folder,move,rename,(script,output,max)->run(script,output,max,null,6*60*60));
    }
    String aliasOperation(String script) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try{run("exec sh -s",out,16384,script.getBytes(StandardCharsets.UTF_8));}catch(Exception error){String code=new String(out.toByteArray(),StandardCharsets.UTF_8).trim();if(code.matches("ALIAS_[A-Z_]+"))throw new IOException(code);throw error;}
        return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
    private byte[] bytes(String script,int max) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();run(script,out,max);return out.toByteArray();}
    static String command(WorkspaceStore.Session s,String script) {
        String e=environment(s);String bash="bash --noprofile --norc -c "+SessionShell.quote(script);
        if(e.startsWith("proot:")){String alias=e.substring(6);if(!WorkspaceLocalFilePicker.validAlias(alias))throw new IllegalArgumentException("INVALID_ENVIRONMENT");return "proot-distro "+WorkspaceEnvironments.verb(alias)+" "+(s.prootIsolated?"--isolated ":"")+SessionShell.quote(alias)+" -- /bin/sh -c "+SessionShell.quote(script);}
        if(e.equals("ubuntu"))return "proot-distro login ubuntu -- /bin/"+bash;
        if(!e.equals("ssh"))return bash;
        if(!SshSessionShell.validHost(s.sshHost)||!SshSessionShell.validUser(s.sshUser)||!SshSessionShell.validPort(s.sshPort))throw new IllegalArgumentException(WorkspaceText.tr("Укажите корректные параметры SSH"));
        String options=" -o ConnectTimeout=10 -o ServerAliveInterval=15 -o ServerAliveCountMax=2 -o StrictHostKeyChecking=yes -o ControlMaster=no -o ControlPath=none";
        if(s.sshAuth.equals("key")) {
            if(!SshSessionShell.validIdentity(s.sshIdentity))throw new IllegalArgumentException(WorkspaceText.tr("Некорректный путь SSH-ключа"));
            options+=" -o IdentitiesOnly=yes -i "+target(s.sshIdentity);
        }
        if(s.sshAuth.equals("key")&&!s.sshEncryptedPassword.isEmpty())options+=" -o PreferredAuthentications=publickey -o PasswordAuthentication=no -o KbdInteractiveAuthentication=no -o NumberOfPasswordPrompts=1";
        options+=s.sshAuth.equals("key")&&!s.sshEncryptedPassword.isEmpty()?" -o BatchMode=no":s.sshAuth.equals("password")?" -o BatchMode=no -o PreferredAuthentications=password -o PubkeyAuthentication=no -o KbdInteractiveAuthentication=no -o NumberOfPasswordPrompts=1":" -o BatchMode=yes";
        return "ssh -T"+options+" -p "+s.sshPort+" -l "+SessionShell.quote(s.sshUser)+" -- "+SessionShell.quote(s.sshHost)+" "+SessionShell.quote(bash);
    }
    private void run(String script,OutputStream out,long max) throws Exception {run(script,out,max,null);}
    private void run(String script,OutputStream out,long max,byte[] input) throws Exception {run(script,out,max,input,90);}
    private void run(String script,OutputStream out,long max,byte[] input,int timeoutSeconds) throws Exception {
        if(closed||serverKnownHosts!=null&&ServerConnection.disconnected(activity,profile.id))throw new IOException(WorkspaceText.tr("Окно закрыто"));
        String invocation=command(profile,script);
        if(serverKnownHosts!=null&&environment(profile).equals("ssh"))invocation=invocation.replaceFirst("ssh -T",java.util.regex.Matcher.quoteReplacement("ssh -T -o "+SessionShell.quote("UserKnownHostsFile="+serverKnownHosts.getAbsolutePath())+" -o GlobalKnownHostsFile=/dev/null"));
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c","exec "+invocation);
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)) {int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        SshPasswordBroker broker=null;
        try {
            if(environment(profile).equals("ssh")&&(profile.sshAuth.equals("password")||profile.sshAuth.equals("key")&&!profile.sshEncryptedPassword.isEmpty())) {
                char[] secret=passwordCopy();if(secret==null&&!profile.sshEncryptedPassword.isEmpty())secret=SshPasswordVault.decrypt(profile);
                if(secret==null)throw new IOException(WorkspaceText.tr("Введите пароль для просмотра файлов"));
                try {broker=new SshPasswordBroker(activity,secret,profile.sshAuth.equals("key")?SshKeyFiles.identity(profile.sshIdentity):null);}finally{Arrays.fill(secret,'\0');}
                File askpass=SessionShell.prepareAskpass(activity.getFilesDir(),profile,activity.getApplicationInfo().sourceDir);
                builder.environment().put("SSH_ASKPASS",askpass.getAbsolutePath());builder.environment().put("SSH_ASKPASS_REQUIRE","force");builder.environment().put("DISPLAY",":0");builder.environment().put("TERMUX_JOBS_SSH_SOCKET",broker.address);builder.environment().put("LC_ALL","C");
            }
            builder.redirectError(new File("/dev/null"));
            try(InputStream in=transport.open(builder::start,input,max,timeoutSeconds*1000L)) {
                if(closed||serverKnownHosts!=null&&ServerConnection.disconnected(activity,profile.id))throw new IOException("CANCELLED");
                byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            }catch(IOException error){throw transportError(error);}
        } finally {if(broker!=null)broker.close();}
    }
    private IOException transportError(IOException error){
        String code=error.getMessage();
        if("FILE_LIMIT".equals(code))return new IOException(WorkspaceText.tr("Файл или список слишком большой"),error);
        if("CANCELLED".equals(code))return new IOException(WorkspaceText.tr("Операция отменена"),error);
        if("FILE_OPERATION_TIMEOUT".equals(code))return new IOException(WorkspaceLanguage.code(activity).equals("ru")?"Время файловой операции истекло. Повторите попытку.":"File operation timed out. Please try again.",error);
        if("FILE_OPERATION_BUSY".equals(code))return new IOException(WorkspaceLanguage.code(activity).equals("ru")?"Другая файловая операция ещё выполняется.":"Another file operation is still running.",error);
        if("SOURCE_UNAVAILABLE".equals(code))return new IOException(environment(profile).equals("ssh")?WorkspaceText.tr("SSH недоступен: проверьте вход, пароль и доверенный ключ сервера в терминале"):WorkspaceText.tr("Каталог или файл недоступен; проверьте путь и выбранную среду"),error);
        return error;
    }

    void localOperation(String script)throws Exception{if(environment(profile).equals("ssh"))throw new IOException("LOCAL_DESTINATION_REQUIRED");bytes(script,1024);}
    InputStream openUpload(Entry entry)throws Exception {return openLocalRead(entry,ServerSftp.MAX_BYTES);}
    InputStream openLocalRead(Entry entry,long maxBytes)throws Exception {
        String env=environment(profile);if(env.equals("ssh"))throw new IOException("LOCAL_SOURCE_REQUIRED");
        if(entry.directory||entry.size<0||entry.size>maxBytes)throw new IOException("FILE_LIMIT");
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c","exec "+command(profile,"test -f "+target(entry.path)+" && exec cat -- "+target(entry.path)));
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)){int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        builder.redirectError(new File("/dev/null"));
        return transport.open(builder::start,null,maxBytes,6*60*60*1000L);
    }
    public void close() {
        closed=true;transport.close();
        synchronized(this){if(password!=null){Arrays.fill(password,'\0');password=null;}}
    }
}
