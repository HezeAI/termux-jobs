package com.termux.app.workspace;

import android.app.AlertDialog;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.util.*;

/** Shared local picker. Only installed proot-distro roots are offered; no environment installation. */
final class WorkspaceLocalFilePicker implements AutoCloseable {
    static final class Location {
        final String name,environment,path;final boolean shared;
        Location(String n,String e,String p,boolean s){name=n;environment=e;path=p;shared=s;}
    }
    static boolean validAlias(String alias){return alias!=null&&alias.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}");}
    static List<Location> locations(File home,File proot,String storageLabel){
        List<Location> result=new ArrayList<>();result.add(new Location("Termux","termux",home.getPath(),false));
        Set<String> aliases=new TreeSet<>();
        for(String name:new String[]{"installed-rootfs","containers"}){File[] roots=new File(proot,name).listFiles();if(roots==null)continue;for(File root:roots){File fs=name.equals("containers")?new File(root,"rootfs"):root;try{if(validAlias(root.getName())&&fs.isDirectory()&&root.getCanonicalFile().equals(new File(root.getParentFile().getCanonicalFile(),root.getName()))&&fs.getCanonicalFile().equals(new File(fs.getParentFile().getCanonicalFile(),fs.getName())))aliases.add(root.getName());}catch(IOException ignored){}}}
        for(String alias:aliases)result.add(new Location(alias.equals("ubuntu")?"Ubuntu":alias,"proot:"+alias,"/",false));
        result.add(new Location(storageLabel,"termux","/sdcard",true));return result;
    }
    private int chatLimit;
    private final TermuxActivity activity;private final List<WorkspaceFileBrowser> browsers=new ArrayList<>();private AlertDialog chooser;private boolean closed;private int selectionEpoch;
    private final java.util.function.Consumer<Runnable> requestStorage;
    WorkspaceLocalFilePicker(TermuxActivity a){this(a,a::requestWorkspaceStorageAccess);}
    WorkspaceLocalFilePicker(TermuxActivity a,java.util.function.Consumer<Runnable> storage){activity=a;requestStorage=storage;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void choose(String title,WorkspaceFileBrowser.FileChoice choice){choose(title,choice,null,null);}
    void choosePhoneFile(WorkspaceFileBrowser.FileChoice choice){
        if(closed)return;final int epoch=++selectionEpoch;
        requestStorage.accept(()->{
            if(closed||epoch!=selectionEpoch)return;
            WorkspaceStore.Session source=new WorkspaceStore.Session();source.name=t("Память устройства","Device storage");source.environment="termux";
            WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,source,"/sdcard",null);
            browser.setRootBoundary("/sdcard");
            browser.setFileChoice((profile,entry)->{if(!closed&&epoch==selectionEpoch)choice.selected(profile,entry);});browsers.add(browser);browser.show();
        });
    }
    void chooseChatAttachments(int limit,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> choice,Runnable gallery){
        if(closed)return;chatLimit=limit;final int epoch=++selectionEpoch;
        chooser=WorkspaceDialogs.builder(activity).setTitle(t("Прикрепить файл","Attach file")).setItems(new String[]{t("Окружения","Environments"),t("Память телефона","Phone storage"),t("Из галереи устройства","From device gallery")},(d,which)->{
            if(closed||epoch!=selectionEpoch)return;
            if(which==2){gallery.run();return;}
            if(which==1){requestStorage.accept(()->{if(closed||epoch!=selectionEpoch)return;WorkspaceStore.Session source=new WorkspaceStore.Session();source.name=t("Память устройства","Device storage");source.environment="termux";WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,source,"/sdcard",null);browser.setRootBoundary("/sdcard");browser.setChatFileChoices((p,entries)->{if(!closed&&epoch==selectionEpoch)choice.accept(p,entries);},limit);browsers.add(browser);browser.show();});}
            else choose(t("Выберите окружение","Choose environment"),null,choice,null,false);
        }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
    }
    void chooseChatAttachment(WorkspaceFileBrowser.FileChoice choice){
        if(closed)return;final int epoch=++selectionEpoch;
        chooser=WorkspaceDialogs.builder(activity).setTitle(t("Прикрепить файл","Attach file")).setItems(new String[]{t("Окружения","Environments"),t("Память телефона","Phone storage")},(d,which)->{
            if(closed||epoch!=selectionEpoch)return;
            if(which==1)choosePhoneFile(choice);else choose(t("Выберите окружение","Choose environment"),choice,null,null,false);
        }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
    }
    void chooseDirectory(String title,java.util.function.BiConsumer<WorkspaceStore.Session,String> choice){choose(title,null,null,choice);}
    void chooseMultiple(String title,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> choice){chatLimit=0;choose(title,null,choice,null);}
    private void choose(String title,WorkspaceFileBrowser.FileChoice choice,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> multiple,java.util.function.BiConsumer<WorkspaceStore.Session,String> directory){choose(title,choice,multiple,directory,true);}
    private void choose(String title,WorkspaceFileBrowser.FileChoice choice,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> multiple,java.util.function.BiConsumer<WorkspaceStore.Session,String> directory,boolean includePhone){
        if(closed)return;final int epoch=++selectionEpoch;
        List<Location> locations=locations(new File(TermuxConstants.TERMUX_HOME_DIR_PATH),new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),t("Память устройства","Device storage"));
        if(!includePhone)locations.removeIf(location->location.shared);
        String[] names=new String[locations.size()];for(int i=0;i<names.length;i++)names[i]=locations.get(i).name;
        chooser=WorkspaceDialogs.builder(activity).setTitle(title).setItems(names,(d,which)->{
            Location location=locations.get(which);Runnable open=()->{if(closed||epoch!=selectionEpoch)return;WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.name=location.name;profile.environment=location.environment;
                WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,profile,location.path,directory==null?null:path->{if(!closed&&epoch==selectionEpoch)directory.accept(profile,path);});if(multiple!=null){if(chatLimit>0)browser.setChatFileChoices((source,entries)->{if(!closed&&epoch==selectionEpoch)multiple.accept(source,entries);},chatLimit);else browser.setFileChoices((source,entries)->{if(!closed&&epoch==selectionEpoch)multiple.accept(source,entries);});}else if(choice!=null)browser.setFileChoice((source,entry)->{if(!closed&&epoch==selectionEpoch)choice.selected(source,entry);});browsers.add(browser);browser.show();};
            if(location.shared)requestStorage.accept(open);else open.run();
        }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
    }
    void cancelSelection(){selectionEpoch++;if(chooser!=null)chooser.dismiss();for(WorkspaceFileBrowser browser:new ArrayList<>(browsers))browser.close();browsers.clear();}
    @Override public void close(){closed=true;cancelSelection();}
}
