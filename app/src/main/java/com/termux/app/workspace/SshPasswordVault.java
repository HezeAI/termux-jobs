package com.termux.app.workspace;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Ciphertext only in workspace JSON. The encryption key never leaves Android Keystore. */
public final class SshPasswordVault {
    private static final String ALIAS = "termux.jobs.ssh.password.v1";
    private SshPasswordVault() {}
    private static SecretKey key(boolean create) throws GeneralSecurityException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        try { store.load(null); } catch (java.io.IOException e) { throw new GeneralSecurityException(e); }
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        if (!create) throw new GeneralSecurityException("Password key unavailable");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    static byte[] binding(WorkspaceStore.Session s) {
        return (s.id + "\n" + s.sshHost + "\n" + s.sshUser + "\n" + s.sshPort).getBytes(StandardCharsets.UTF_8);
    }
    public static String encrypt(WorkspaceStore.Session s, char[] password) throws GeneralSecurityException {
        return encrypt(key(true), binding(s), password);
    }
    public static char[] decrypt(WorkspaceStore.Session s) throws GeneralSecurityException {
        return decrypt(key(false), binding(s), s.sshEncryptedPassword);
    }
    static String encrypt(SecretKey key, byte[] binding, char[] password) throws GeneralSecurityException {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] plain = new byte[encoded.remaining()]; encoded.get(plain);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key); cipher.updateAAD(binding);
            return "1:" + Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(cipher.doFinal(plain), Base64.NO_WRAP);
        } finally { Arrays.fill(plain, (byte) 0); if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0); }
    }
    static char[] decrypt(SecretKey key, byte[] binding, String value) throws GeneralSecurityException {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || !parts[0].equals("1")) throw new GeneralSecurityException("Invalid password record");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.decode(parts[1], Base64.NO_WRAP)));
            cipher.updateAAD(binding); byte[] plain = cipher.doFinal(Base64.decode(parts[2], Base64.NO_WRAP));
            try { CharBuffer chars = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(plain)); char[] result = new char[chars.remaining()];
                chars.get(result); if (chars.hasArray()) Arrays.fill(chars.array(), '\0'); return result;
            } finally { Arrays.fill(plain, (byte) 0); }
        } catch (IllegalArgumentException e) { throw new GeneralSecurityException("Invalid password record", e); }
    }
}
