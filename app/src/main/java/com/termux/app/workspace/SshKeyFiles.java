package com.termux.app.workspace;

import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Local key import: private directory, exclusive filename, bounded input; never logs contents. */
final class SshKeyFiles {
    static final long MAX=256*1024;
    static String identity(String path){return path.startsWith("~/")?TermuxConstants.TERMUX_HOME_DIR_PATH+path.substring(1):path;}
    static boolean privateTermuxFile(String path)throws IOException {
        String canonical=new File(identity(path)).getCanonicalPath();
        return canonical.startsWith(new File(TermuxConstants.TERMUX_FILES_DIR_PATH).getCanonicalPath()+"/");
    }
    static void validate(File file)throws IOException {
        if(!file.isFile()||!file.canRead()||file.length()==0||file.length()>MAX)throw new IOException("INVALID_KEY_FILE");
        byte[] header=new byte[80];int count;try(InputStream in=new FileInputStream(file)){count=in.read(header);}
        String first=new String(header,0,Math.max(0,count),StandardCharsets.US_ASCII).split("\r?\n",2)[0];
        if(!first.matches("-----BEGIN (OPENSSH |RSA |DSA |EC |ENCRYPTED )?PRIVATE KEY-----"))throw new IOException("PRIVATE_KEY_REQUIRED");
    }
    private static void ownerOnly(File file,boolean directory)throws IOException {
        if(!file.setReadable(false,false)||!file.setWritable(false,false)||!file.setExecutable(false,false)||!file.setReadable(true,true)||!file.setWritable(true,true)||(directory&&!file.setExecutable(true,true)))throw new IOException("KEY_PERMISSIONS_FAILED");
    }
    static File createPrivate(File files)throws IOException {
        File directory=new File(files,"server-keys");
        if(!directory.getCanonicalFile().equals(new File(files.getCanonicalFile(),"server-keys")))throw new IOException("INVALID_KEY_DIRECTORY");
        if(!directory.isDirectory()&&!directory.mkdir())throw new IOException("INVALID_KEY_DIRECTORY");
        ownerOnly(directory,true);
        File key=File.createTempFile("identity-",".key",directory);
        try{ownerOnly(key,false);return key;}catch(IOException error){key.delete();throw error;}
    }
}
