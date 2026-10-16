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
    private final TermuxActivity activity;private final List<WorkspaceFileBrowser> browsers=new ArrayList<>();private AlertDialog chooser;private boolean closed;
    WorkspaceLocalFilePicker(TermuxActivity a){activity=a;}
    private String t(String ru,String en){return WorkspaceLanguage.code(activity).equals("ru")?ru:en;}
    void choose(String title,WorkspaceFileBrowser.FileChoice choice){choose(title,choice,null,null);}
    void chooseDirectory(String title,java.util.function.BiConsumer<WorkspaceStore.Session,String> choice){choose(title,null,null,choice);}
    void chooseMultiple(String title,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> choice){choose(title,null,choice,null);}
    private void choose(String title,WorkspaceFileBrowser.FileChoice choice,java.util.function.BiConsumer<WorkspaceStore.Session,List<ProjectFiles.Entry>> multiple,java.util.function.BiConsumer<WorkspaceStore.Session,String> directory){
        if(closed)return;
        List<Location> locations=locations(new File(TermuxConstants.TERMUX_HOME_DIR_PATH),new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro"),t("Память устройства","Device storage"));
        String[] names=new String[locations.size()];for(int i=0;i<names.length;i++)names[i]=locations.get(i).name;
        chooser=WorkspaceDialogs.builder(activity).setTitle(title).setItems(names,(d,which)->{
            Location location=locations.get(which);Runnable open=()->{if(closed)return;WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.name=location.name;profile.environment=location.environment;
                WorkspaceFileBrowser browser=new WorkspaceFileBrowser(activity,profile,location.path,directory==null?null:path->directory.accept(profile,path));if(multiple!=null)browser.setFileChoices(multiple);else browser.setFileChoice(choice);browsers.add(browser);browser.show();};
            if(location.shared)activity.requestWorkspaceStorageAccess(open);else open.run();
        }).setNegativeButton(t("Отмена","Cancel"),null).create();chooser.show();
    }
    @Override public void close(){closed=true;if(chooser!=null)chooser.dismiss();for(WorkspaceFileBrowser browser:browsers)browser.close();}
}
