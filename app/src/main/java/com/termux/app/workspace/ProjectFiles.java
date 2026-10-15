package com.termux.app.workspace;

import android.app.Activity;
import com.termux.shared.shell.TermuxShellUtils;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Bounded, independent file operations. Never types into the user's terminal. */
final class ProjectFiles implements AutoCloseable {
    static final int MAX_LIST = 2 * 1024 * 1024, MAX_PREVIEW = 256 * 1024;
    static final long MAX_EXPORT = 256L * 1024 * 1024;
    final Activity activity; final WorkspaceStore.Session profile;
    private volatile Process process; private volatile boolean closed;
    private char[] password;
    private File serverKnownHosts;
    void setServerKnownHosts(File file){serverKnownHosts=file;}
    ProjectFiles(Activity activity, WorkspaceStore.Session profile) {this.activity=activity;this.profile=profile.copy();this.profile.id=profile.id;
        this.profile.name=profile.name;this.profile.handle=profile.handle;this.profile.codexSocketId=profile.codexSocketId;
        this.profile.sshEncryptedPassword=profile.sshEncryptedPassword;}
    void setPassword(char[] value) {if(password!=null)Arrays.fill(password,'\0');password=value.clone();}
    static String environment(WorkspaceStore.Session s) {return s.environment.equals("codex")?s.codexEnvironment:s.environment.equals("legacy")?"termux":s.environment;}
    static String environmentLabel(WorkspaceStore.Session s) {String e=environment(s);return e.equals("ubuntu")?"Ubuntu":e.equals("ssh")?"SSH · "+s.sshHost:"Termux";}
    static String target(String path) {
        if(path==null || path.indexOf('\0')>=0 || path.contains("\n") || path.contains("\r"))throw new IllegalArgumentException(WorkspaceText.tr("Некорректный путь"));
        if(path.isEmpty() || path.equals("~"))return "\"$HOME\"";
        return path.startsWith("~/")?"\"$HOME\"/"+SessionShell.quote(path.substring(2)):SessionShell.quote(path);
    }
    static String listScript(String path) {
        return "cd -- "+target(path)+" || exit 2; printf '%s\\0' \"$(pwd -P)\"; exec find -L . -mindepth 1 -maxdepth 1 -printf '%y\\0%f\\0%s\\0'";
    }
    static final class Entry {
        final String name,path;final boolean directory;final long size;
        Entry(String parent,String name,boolean directory,long size) {this.name=name;this.path=(parent.equals("/")?"/":parent+"/")+name;this.directory=directory;this.size=size;}
    }
    static final class Listing {String path; final List<Entry> entries=new ArrayList<>();}
    static Listing parse(byte[] bytes) throws IOException {
        String[] fields=new String(bytes,StandardCharsets.UTF_8).split("\u0000",-1);
        if(fields.length<2 || !fields[0].startsWith("/") || (fields.length-2)%3!=0)throw new IOException(WorkspaceText.tr("Некорректный список файлов"));
        Listing result=new Listing();result.path=fields[0];
        for(int i=1;i+2<fields.length;i+=3) {
            if(fields[i+1].isEmpty() || fields[i+1].contains("/") || fields[i+1].equals(".") || fields[i+1].equals(".."))throw new IOException(WorkspaceText.tr("Некорректное имя файла"));
            long size;try {size=Long.parseLong(fields[i+2]);}catch(NumberFormatException e){throw new IOException(WorkspaceText.tr("Некорректный размер файла"));}
            if(fields[i].equals("d") || fields[i].equals("f"))result.entries.add(new Entry(result.path,fields[i+1],fields[i].equals("d"),size));
            if(result.entries.size()>4000)throw new IOException(WorkspaceText.tr("Слишком много файлов; выберите вложенную папку"));
        }
        result.entries.sort(Comparator.comparing((Entry e)->!e.directory).thenComparing(e->e.name.toLowerCase(Locale.ROOT)));return result;
    }
    static String mime(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.endsWith(".apk"))return "application/vnd.android.package-archive";
        if(n.endsWith(".png"))return "image/png";if(n.endsWith(".jpg")||n.endsWith(".jpeg"))return "image/jpeg";
        if(n.endsWith(".webp"))return "image/webp";if(n.endsWith(".gif"))return "image/gif";
        String ext=n.lastIndexOf('.')<0?"":n.substring(n.lastIndexOf('.')+1);
        if(Arrays.asList("md","txt","log","json","xml","yml","yaml","java","kt","kts","gradle","sh","py","js","ts","css","html","rs","toml","properties","c","h","cpp","gitignore").contains(ext)||n.equals("makefile")||n.equals("dockerfile"))return "text/plain";
        return "application/octet-stream";
    }
    static boolean visible(Entry e,String filter) {return e.directory || filter.equals(WorkspaceText.tr("Все")) || filter.equals(WorkspaceText.tr("Текст"))&&mime(e.name).equals("text/plain") || filter.equals(WorkspaceText.tr("Изображения"))&&mime(e.name).startsWith("image/") || filter.equals("APK")&&e.name.toLowerCase(Locale.ROOT).endsWith(".apk");}
    Listing list(String path) throws Exception {return parse(bytes(listScript(path),MAX_LIST));}
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
        if(e.size>max)throw new IOException(WorkspaceText.tr("Файл превышает лимит экспорта 256 МБ"));
        try(OutputStream out=new FileOutputStream(destination)) {run("test -f "+target(e.path)+" && exec cat -- "+target(e.path),out,max);}
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
    byte[] packageDescription(PackageCatalog.Entry entry,boolean installed) throws Exception {
        try{return bytes(PackageCatalog.descriptionQuery(entry,installed),256*1024);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Описание недоступно. Обновите каталог или проверьте выбранное окружение."));}
    }
    byte[] packageVersions(String name) throws Exception {
        try {return bytes(PackageCatalog.versionsQuery(name),1024*1024);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Не удалось прочитать доступные версии пакета. Проверьте APT и выбранное окружение."));}
    }
    byte[] packageQuery(int tab) throws Exception {
        try {return bytes(PackageCatalog.query(tab),PackageCatalog.MAX_BYTES);}
        catch(Exception error){if(closed)throw error;throw new IOException(WorkspaceText.tr("Не удалось прочитать пакеты. Проверьте наличие APT/dpkg и доступность выбранного окружения."));}
    }
    private byte[] bytes(String script,int max) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();run(script,out,max);return out.toByteArray();}
    static String command(WorkspaceStore.Session s,String script) {
        String e=environment(s);String bash="bash --noprofile --norc -c "+SessionShell.quote(script);
        if(e.equals("ubuntu"))return "proot-distro login ubuntu -- /bin/"+bash;
        if(!e.equals("ssh"))return bash;
        if(!SshSessionShell.validHost(s.sshHost)||!SshSessionShell.validUser(s.sshUser)||!SshSessionShell.validPort(s.sshPort))throw new IllegalArgumentException(WorkspaceText.tr("Укажите корректные параметры SSH"));
        String options=" -o ConnectTimeout=10 -o StrictHostKeyChecking=yes -o ControlMaster=no -o ControlPath=none";
        if(s.sshAuth.equals("key")) {
            if(!SshSessionShell.validIdentity(s.sshIdentity))throw new IllegalArgumentException(WorkspaceText.tr("Некорректный путь SSH-ключа"));
            options+=" -o IdentitiesOnly=yes -i "+target(s.sshIdentity);
        }
        options+=s.sshAuth.equals("password")?" -o BatchMode=no -o PreferredAuthentications=password -o PubkeyAuthentication=no -o KbdInteractiveAuthentication=no -o NumberOfPasswordPrompts=1":" -o BatchMode=yes";
        return "ssh -T"+options+" -p "+s.sshPort+" -l "+SessionShell.quote(s.sshUser)+" -- "+SessionShell.quote(s.sshHost)+" "+SessionShell.quote(bash);
    }
    private void run(String script,OutputStream out,long max) throws Exception {run(script,out,max,null);}
    private synchronized void run(String script,OutputStream out,long max,byte[] input) throws Exception {
        if(closed)throw new IOException(WorkspaceText.tr("Окно закрыто"));
        String invocation=command(profile,script);
        if(serverKnownHosts!=null&&environment(profile).equals("ssh"))invocation=invocation.replaceFirst("ssh -T",java.util.regex.Matcher.quoteReplacement("ssh -T -o "+SessionShell.quote("UserKnownHostsFile="+serverKnownHosts.getAbsolutePath())+" -o GlobalKnownHostsFile=/dev/null"));
        ProcessBuilder builder=new ProcessBuilder(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH+"/bash","--noprofile","--norc","-c","exec "+invocation);
        builder.directory(new File(TermuxConstants.TERMUX_HOME_DIR_PATH));builder.environment().clear();
        for(String item:TermuxShellUtils.buildEnvironment(activity,false,TermuxConstants.TERMUX_HOME_DIR_PATH)) {int eq=item.indexOf('=');if(eq>0)builder.environment().put(item.substring(0,eq),item.substring(eq+1));}
        SshPasswordBroker broker=null;
        ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
        try {
            if(environment(profile).equals("ssh")&&profile.sshAuth.equals("password")) {
                char[] secret=password==null?profile.sshEncryptedPassword.isEmpty()?null:SshPasswordVault.decrypt(profile):password.clone();
                if(secret==null)throw new IOException(WorkspaceText.tr("Введите пароль для просмотра файлов"));
                try {broker=new SshPasswordBroker(activity,secret);}finally{Arrays.fill(secret,'\0');}
                File askpass=SessionShell.prepareAskpass(activity.getFilesDir(),profile,activity.getApplicationInfo().sourceDir);
                builder.environment().put("SSH_ASKPASS",askpass.getAbsolutePath());builder.environment().put("SSH_ASKPASS_REQUIRE","force");builder.environment().put("DISPLAY",":0");builder.environment().put("TERMUX_JOBS_SSH_SOCKET",broker.address);builder.environment().put("LC_ALL","C");
            }
            builder.redirectError(new File("/dev/null"));Process owned=builder.start();process=owned;
            timer.schedule(owned::destroy,90,TimeUnit.SECONDS);
            try(OutputStream stdin=owned.getOutputStream()){if(input!=null)stdin.write(input);}
            try(InputStream in=owned.getInputStream()) {byte[] buffer=new byte[32768];int n;long total=0;while((n=in.read(buffer))!=-1){if(closed||Thread.currentThread().isInterrupted())throw new IOException(WorkspaceText.tr("Операция отменена"));total+=n;if(total>max)throw new IOException(WorkspaceText.tr("Файл или список слишком большой"));out.write(buffer,0,n);}}
            if(owned.waitFor()!=0)throw new IOException(environment(profile).equals("ssh")?WorkspaceText.tr("SSH недоступен: проверьте вход, пароль и доверенный ключ сервера в терминале"):WorkspaceText.tr("Каталог или файл недоступен; проверьте путь и выбранную среду"));
        } finally {if(process!=null)process.destroy();process=null;timer.shutdownNow();if(broker!=null)broker.close();}
    }
    public void close() {closed=true;Process owned=process;if(owned!=null)owned.destroy();if(password!=null)Arrays.fill(password,'\0');}
}
