package com.termux.app.workspace;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class SshSessionShellTest {
    private static String read(java.io.InputStream input) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
        while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
    @Test public void explicitModesUseCorrectSshOptionsWithoutEmbeddingPasswords() {
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.sshHost = "example.invalid"; s.sshAuth = "password";
        s.sshEncryptedPassword = "encrypted-dummy-test";
        String command = SshSessionShell.command(s);
        assertTrue(command.contains("PreferredAuthentications=password")); assertTrue(command.contains("PubkeyAuthentication=no"));
        assertTrue(command.contains("StrictHostKeyChecking=ask")); assertTrue(command.contains("NumberOfPasswordPrompts=1"));
        assertFalse(command.contains("encrypted-dummy-test"));
        s.sshAuth = "key"; s.sshIdentity = "~/key ' with spaces";
        assertTrue(SshSessionShell.command(s).contains("IdentitiesOnly=yes"));
        assertTrue(SshSessionShell.command(s).contains("\"$HOME\"/"));
        assertFalse(SshSessionShell.validIdentity("/tmp/key\nmalicious"));
    }
    @Test public void validatesConnectionArguments() {
        assertTrue(SshSessionShell.validHost("server.example"));
        assertTrue(SshSessionShell.validHost("2001:db8::1"));
        for (String host : new String[]{"", "-oProxyCommand=sh", "root@host", "host; echo bad", "host\nother"}) assertFalse(SshSessionShell.validHost(host));
        assertFalse(SshSessionShell.validUser("root;id"));
        assertFalse(SshSessionShell.validPort(0)); assertFalse(SshSessionShell.validPort(65536));
    }
    @Test public void quotedRemoteCommandsKeepDirectoryVariablesAndStopAfterFailure() throws Exception {
        File home = Files.createTempDirectory("ssh-fixture").toFile();
        File project = new File(home, "project ' with spaces"); assertTrue(project.mkdir());
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.environment = "ssh";
        s.sshHost = "example.invalid"; s.sshUser = "root"; s.sshKeepOpen = false;
        s.directory = "~/project ' with spaces";
        s.startup = "test \"$PWD\" = \"$HOME/project ' with spaces\"; export STEP='one two'";
        s.startup2 = "printf '%s\\n' \"$STEP\"; false";
        s.startup3 = "printf 'SHOULD_NOT_RUN'";
        ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", SshSessionShell.remoteScript(s));
        builder.environment().put("HOME", home.toString()); builder.redirectErrorStream(true);
        Process process = builder.start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        String output = read(process.getInputStream());
        assertEquals(1, process.exitValue()); assertTrue(output.contains("one two"));
        assertTrue(output.contains("Ошибка команды 2")); assertFalse(output.contains("SHOULD_NOT_RUN"));
        assertTrue(project.delete()); assertTrue(home.delete());
    }
    @Test public void sshReceivesOneRemotePayloadWithoutLocalExpansion() throws Exception {
        File dir = Files.createTempDirectory("ssh-argv").toFile(); File fake = new File(dir, "ssh");
        Files.write(fake.toPath(), "#!/bin/bash\nprintf '%s\\n' \"$@\"\n".getBytes(StandardCharsets.UTF_8)); assertTrue(fake.setExecutable(true));
        WorkspaceStore.Session s = new WorkspaceStore.Session(); s.sshHost = "my-host"; s.sshUser = "deploy"; s.sshPort = 2222;
        s.directory = "~"; s.startup = "printf '%s\\n' \"$HOME\"";
        ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", SshSessionShell.command(s));
        builder.environment().put("PATH", dir.toString()); builder.redirectErrorStream(true);
        Process process = builder.start(); assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        String args = read(process.getInputStream());
        assertEquals(0, process.exitValue()); assertTrue(args.startsWith("-t\n-p\n2222\n-l\ndeploy\n--\nmy-host\nbash -ic "));
        assertTrue(args.contains("$HOME")); assertTrue(args.contains("exec bash -i"));
        assertTrue(fake.delete()); assertTrue(dir.delete());
    }
}
