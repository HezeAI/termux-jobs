package com.termux.app.workspace;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;

/** Per-profile ciphertext in the library; one private handoff file consumed by the owned CLI. */
final class GeminiApiKeyVault {
    private static final String ALIAS="termux.jobs.gemini.api.v1";
    private static SecretKey key(boolean create)throws GeneralSecurityException{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");try{store.load(null);}catch(IOException e){throw new GeneralSecurityException(e);}
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        if(!create)throw new GeneralSecurityException("API key unavailable");
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());return generator.generateKey();
    }
    static byte[] binding(WorkspaceStore.Session p){return ("gemini-api-v1\n"+p.id).getBytes(StandardCharsets.UTF_8);}
    static boolean valid(char[] value){if(value.length==0||value.length>4096)return false;for(char c:value)if(c<33||c>126)return false;return true;}
    static boolean validRecord(String value){return value.isEmpty()||value.length()<=8192&&value.matches("1:[A-Za-z0-9+/]{16}:[A-Za-z0-9+/]+=*");}
    static String encrypt(WorkspaceStore.Session p,char[] value)throws GeneralSecurityException{return encrypt(key(true),p,value);}
    static String encrypt(SecretKey key,WorkspaceStore.Session p,char[] value)throws GeneralSecurityException{if(!valid(value))throw new GeneralSecurityException("Invalid API key");return SshPasswordVault.encrypt(key,binding(p),value);}
    static char[] decrypt(SecretKey key,WorkspaceStore.Session p)throws GeneralSecurityException{return SshPasswordVault.decrypt(key,binding(p),p.geminiEncryptedKey);}
    static File runtimePath(File files,WorkspaceStore.Session p){java.util.UUID.fromString(p.id);return new File(new File(GeminiCli.directory(files),"keys"),p.id+".key");}
    static File runtime(Context c,WorkspaceStore.Session p)throws IOException{return runtimePath(c.getFilesDir(),p);}
    static void prepare(Context c,WorkspaceStore.Session p)throws IOException,GeneralSecurityException{
        File target=runtime(c,p);if(target.exists()&&!target.delete())throw new IOException("Cannot clear API handoff");
        if(!p.geminiAuth.equals("api")||p.geminiEncryptedKey.isEmpty())return;
        char[] value=decrypt(key(false),p);try{write(target,value);}finally{Arrays.fill(value,'\0');}
    }
    static void write(File target,char[] value)throws IOException{
        if(!valid(value))throw new IOException("Invalid API key");File directory=target.getParentFile();if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create API handoff");CodexChatControl.protect(directory);if(!directory.setExecutable(false,false)||!directory.setExecutable(true,true))throw new IOException("Cannot protect API handoff");
        File temp=File.createTempFile("api-",".tmp",directory);ByteBuffer encoded=StandardCharsets.UTF_8.encode(CharBuffer.wrap(value));byte[] bytes=new byte[encoded.remaining()];encoded.get(bytes);
        try{CodexChatControl.protect(temp);try(OutputStream out=new FileOutputStream(temp)){out.write(bytes);}if(!temp.renameTo(target))throw new IOException("Cannot publish API handoff");}finally{temp.delete();Arrays.fill(bytes,(byte)0);if(encoded.hasArray())Arrays.fill(encoded.array(),(byte)0);}
    }
    static void discard(Context c,WorkspaceStore.Session p){try{runtime(c,p).delete();}catch(Exception ignored){}}
}
