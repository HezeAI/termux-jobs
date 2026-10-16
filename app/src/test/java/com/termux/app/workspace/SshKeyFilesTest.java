package com.termux.app.workspace;

import org.junit.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import static org.junit.Assert.*;

public class SshKeyFilesTest {
    @Rule public org.junit.rules.TemporaryFolder temporary=new org.junit.rules.TemporaryFolder();
    @Test public void importsUseExclusiveNamesAndOwnerOnlyPermissions()throws Exception {
        File a=SshKeyFiles.createPrivate(temporary.getRoot()),b=SshKeyFiles.createPrivate(temporary.getRoot());
        assertNotEquals(a,b);assertTrue(a.exists());assertTrue(b.exists());
        assertEquals("rw-------",PosixFilePermissions.toString(Files.getPosixFilePermissions(a.toPath())));
        assertEquals("rwx------",PosixFilePermissions.toString(Files.getPosixFilePermissions(a.getParentFile().toPath())));
    }
    @Test public void importsRejectSymlinkDirectory()throws Exception {
        File outside=temporary.newFolder("elsewhere");Files.createSymbolicLink(new File(temporary.getRoot(),"server-keys").toPath(),outside.toPath());
        try{SshKeyFiles.createPrivate(temporary.getRoot());fail();}catch(IOException expected){}
        assertEquals(0,outside.list().length);
    }
    @Test public void validatorRejectsPublicKeysAndOversizedFiles()throws Exception {
        File f=temporary.newFile("fixture");Files.write(f.toPath(),"ssh-ed25519 dummy-public-fixture".getBytes("UTF-8"));
        try{SshKeyFiles.validate(f);fail();}catch(IOException expected){}
        Files.write(f.toPath(),"-----BEGIN OPENSSH PRIVATE KEY-----\nfixture-not-a-real-key\n".getBytes("UTF-8"));SshKeyFiles.validate(f);
        try(RandomAccessFile out=new RandomAccessFile(f,"rw")){out.setLength(SshKeyFiles.MAX+1);}
        try{SshKeyFiles.validate(f);fail();}catch(IOException expected){}
    }
    @Test public void keyPassphrasePromptIsBoundToExactKeyAndNeverPasswordPrompt(){
        String path="/private/key with spaces";
        assertTrue(SshPasswordBroker.isKeyPassphrasePrompt(path,"","Enter passphrase for key '"+path+"': "));
        assertFalse(SshPasswordBroker.isKeyPassphrasePrompt(path,"","tester@example.invalid's password:"));
        assertFalse(SshPasswordBroker.isKeyPassphrasePrompt(path,"","Enter passphrase for key '/different':"));
        assertFalse(SshPasswordBroker.isKeyPassphrasePrompt(path,"confirm","Enter passphrase for key '"+path+"':"));
        String longPath="/"+String.join("",Collections.nCopies(150,"x"));
        assertTrue(SshPasswordBroker.isKeyPassphrasePrompt(longPath,"","Enter passphrase for key '"+longPath.substring(0,100)+"': "));
    }
}
