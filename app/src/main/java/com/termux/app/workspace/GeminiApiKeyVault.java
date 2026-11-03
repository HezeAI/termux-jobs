package com.termux.app.workspace;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;

/** Profile-bound ciphertext; plaintext is handed off through a run-owned one-shot broker. */
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
    static GeminiKeyBroker prepare(Context c,WorkspaceStore.Session p)throws IOException,GeneralSecurityException{
        if(!p.geminiAuth.equals("api")||p.geminiEncryptedKey.isEmpty())return null;
        WorkspaceStore.Session captured=new WorkspaceStore.Session();captured.id=p.id;captured.geminiEncryptedKey=p.geminiEncryptedKey;
        return new GeminiKeyBroker(p.geminiSocketId,()->decrypt(key(false),captured));
    }
}
