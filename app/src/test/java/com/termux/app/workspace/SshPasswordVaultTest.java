package com.termux.app.workspace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SshPasswordVaultTest {
    @Test public void encryptionIsRandomizedBoundToProfileAndAuthenticated() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES"); generator.init(256); SecretKey key = generator.generateKey();
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.sshHost = "example.invalid"; s.sshUser = "deploy";
        char[] password = "dummy-TEST-пароль".toCharArray();
        String first = SshPasswordVault.encrypt(key, SshPasswordVault.binding(s), password);
        String second = SshPasswordVault.encrypt(key, SshPasswordVault.binding(s), password);
        assertNotEquals(first, second); assertFalse(first.contains("dummy-TEST"));
        assertArrayEquals(password, SshPasswordVault.decrypt(key, SshPasswordVault.binding(s), first));
        WorkspaceStore.Session other = s.copy();
        try { SshPasswordVault.decrypt(key, SshPasswordVault.binding(other), first); fail("Cross-profile decryption accepted"); }
        catch (java.security.GeneralSecurityException expected) {}
        s.sshHost = "different.invalid";
        try { SshPasswordVault.decrypt(key, SshPasswordVault.binding(s), first); fail("Changed host accepted"); }
        catch (java.security.GeneralSecurityException expected) {}
    }
    @Test public void hostAndUnknownPromptsCannotRequestThePassword() {
        assertTrue(SshPasswordBroker.isPasswordPrompt("", "deploy@example.invalid's password: "));
        assertFalse(SshPasswordBroker.isPasswordPrompt("confirm", "password:"));
        assertFalse(SshPasswordBroker.isPasswordPrompt("", "Are you sure you want to continue connecting (yes/no)?"));
        assertTrue(SshPasswordBroker.isHostPrompt("", "Are you sure you want to continue connecting (yes/no)?"));
        assertFalse(SshPasswordBroker.isPasswordPrompt("", "Enter passphrase for key '/tmp/key': "));
        assertFalse(SshPasswordBroker.isPasswordPrompt("none", "password:"));
    }
}
