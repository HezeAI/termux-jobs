package com.termux.x11;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONObject;
import com.termux.shared.termux.TermuxConstants;

/** Read the actual rootfs journal even when the program catalog is cached. */
final class FirefoxCompatibilityState {
    static final String WRAPPER=".local/bin/termux-jobs-firefox", LAUNCHER=".local/share/applications/firefox.desktop";
    static final String FEDORA_LAUNCHER=".local/share/applications/org.mozilla.firefox.desktop";
    static int read(String target){
        if(!DesktopPrograms.supportsFirefox(target))return 0;
        if(target.equals("termux")){
            try{return readHome(new File(TermuxConstants.TERMUX_FILES_DIR_PATH),new File(TermuxConstants.TERMUX_HOME_DIR_PATH));}catch(Exception e){return -1;}
        }
        File base=new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH,"var/lib/proot-distro");
        File container=new File(base,"containers/"+target),root=new File(container,"rootfs");
        if(Files.isSymbolicLink(container.toPath())||Files.isSymbolicLink(root.toPath()))return -1;
        if(!root.isDirectory())root=new File(base,"installed-rootfs/"+target);
        try{return read(root);}catch(Exception e){return -1;}
    }
    static int read(File root)throws Exception{
        return readHome(root,new File(root,"root"));
    }
    static int readHome(File root,File home)throws Exception{
        File journal=new File(home,".local/state/termux-jobs/firefox-compat.json");
        check(root,journal);if(!journal.exists())return 0;
        JSONObject json=new JSONObject(new String(bytes(journal,8192),java.nio.charset.StandardCharsets.UTF_8));
        Set<String> keys=new HashSet<>();Iterator<String> it=json.keys();while(it.hasNext())keys.add(it.next());
        Set<String> old=new HashSet<>(Arrays.asList(WRAPPER,LAUNCHER));
        if(!keys.equals(old)&&!keys.equals(new HashSet<>(Arrays.asList(WRAPPER,FEDORA_LAUNCHER))))return -1;
        for(String key:keys){File file=new File(home,key);check(root,file);
            if(!file.isFile()||!X11Runtime.digest(bytes(file,65536)).equals(json.getString(key)))return -1;
        }
        return 1;
    }
    private static void check(File root,File file)throws IOException{
        for(File p=file;p!=null;p=p.getParentFile()){
            if(Files.isSymbolicLink(p.toPath()))throw new IOException("Compatibility path is a link");
            if(p.equals(root))return;
        }
        throw new IOException("Compatibility path outside rootfs");
    }
    private static byte[] bytes(File file,int max)throws IOException{
        if(!file.isFile()||file.length()>max)throw new IOException("Invalid compatibility state file");
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>max)throw new IOException("Compatibility state too large");}return out.toByteArray();
        }
    }
}
