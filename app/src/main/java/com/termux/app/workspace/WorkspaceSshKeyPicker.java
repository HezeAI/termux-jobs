package com.termux.app.workspace;

import android.app.AlertDialog;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Explicit local-only key selection/import. No previews, shared-storage exports or SSH commands. */
final class WorkspaceSshKeyPicker implements AutoCloseable {
    private final TermuxActivity activity;
    private final WorkspaceLocalFilePicker localPicker;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<WorkspaceFileBrowser> browsers=new ArrayList<>();
    private final List<AlertDialog> dialogs=new ArrayList<>();
    private final Set<File> imported=Collections.synchronizedSet(new HashSet<>());
    private volatile boolean closed;
    private String retained="";
    WorkspaceSshKeyPicker(TermuxActivity activity){this.activity=activity;localPicker=new WorkspaceLocalFilePicker(activity);}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    private void show(AlertDialog d){dialogs.add(d);d.show();}
    void choose(Consumer<String> selected){
        if(closed)return;
        localPicker.choose(t("Выбрать приватный SSH-ключ","Choose private SSH key"),(source,entry)->select(source,entry,selected));
    }

    private void select(WorkspaceStore.Session source,ProjectFiles.Entry entry,Consumer<String> selected){
        if(closed)return;
        boolean direct=false;try{direct=ProjectFiles.environment(source).equals("termux")&&SshKeyFiles.privateTermuxFile(entry.path);}catch(IOException ignored){}
        if(direct){read(source,entry,false,selected);return;}
        show(WorkspaceDialogs.builder(activity).setTitle(t("Скопировать ключ в Termux?","Copy key into Termux?"))
            .setMessage(t("SSH этого сервера работает в Termux. Ключ из другого окружения или общей памяти будет скопирован в приватный каталог приложения с правами 600. Оригинал останется, существующие ключи не перезаписываются.","This server uses Termux SSH. A key from another environment or shared storage will be copied into the app’s private directory with mode 600. The original stays unchanged; existing keys are not overwritten."))
            .setPositiveButton(t("Скопировать и выбрать","Copy and select"),(d,w)->read(source,entry,true,selected))
            .setNegativeButton(t("Отмена","Cancel"),null).create());
    }
    private void read(WorkspaceStore.Session source,ProjectFiles.Entry entry,boolean copy,Consumer<String> selected){
        if(closed)return;
        AlertDialog progress=WorkspaceDialogs.builder(activity).setMessage(t("Проверка файла ключа…","Checking key file…")).setCancelable(false).create();show(progress);
        worker.execute(()->{
            File result=null;boolean success=false;
            try{
                if(copy){result=SshKeyFiles.createPrivate(activity.getFilesDir());imported.add(result);try(ProjectFiles files=new ProjectFiles(activity,source)){files.export(entry,result,SshKeyFiles.MAX);}}
                else result=new File(entry.path);
                SshKeyFiles.validate(result);success=true;
            }catch(Exception ignored){} // No raw exceptions or file contents in diagnostics.
            if(closed){if(copy&&result!=null){result.delete();imported.remove(result);}return;}
            final File file=result;final boolean ok=success;
            activity.runOnUiThread(()->{
                progress.dismiss();
                if(closed||!ok){if(copy&&file!=null){file.delete();imported.remove(file);}if(!closed)show(WorkspaceDialogs.builder(activity).setMessage(t("Не удалось прочитать приватный ключ. Выберите доступный файл приватного ключа (не .pub) размером до 256 КБ.","Could not read a private key. Choose an accessible private-key file (not .pub), up to 256 KiB.")).setPositiveButton("OK",null).create());return;}
                selected.accept(file.getAbsolutePath());
            });
        });
    }
    void retain(String path){try{retained=path.isEmpty()?"":new File(SshKeyFiles.identity(path)).getCanonicalPath();}catch(IOException error){retained=path;}}
    @Override public void close(){
        if(closed)return;closed=true;localPicker.close();
        for(WorkspaceFileBrowser browser:browsers)browser.close();
        for(AlertDialog dialog:dialogs)if(dialog.isShowing())dialog.dismiss();
        worker.shutdown(); // Only independent, bounded file operations; no user sessions are interrupted.
        synchronized(imported){for(File file:imported){String path;try{path=file.getCanonicalPath();}catch(IOException error){path=file.getAbsolutePath();}if(!path.equals(retained))file.delete();}}
    }
}
