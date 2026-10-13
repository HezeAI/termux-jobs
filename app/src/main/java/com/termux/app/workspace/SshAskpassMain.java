package com.termux.app.workspace;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

/** Started by app_process only for OpenSSH askpass; stdout belongs exclusively to SSH. */
public final class SshAskpassMain {
    private SshAskpassMain() {}
    public static void main(String[] args) {
        int status = 1;
        try (LocalSocket socket = new LocalSocket()) {
            String address = System.getenv("TERMUX_JOBS_SSH_SOCKET");
            if (address == null || !address.startsWith("termux-jobs-ssh-")) throw new java.io.IOException();
            socket.connect(new LocalSocketAddress(address, LocalSocketAddress.Namespace.ABSTRACT));
            socket.setSoTimeout(125000);
            if (socket.getPeerCredentials().getUid() != android.os.Process.myUid()) throw new java.io.IOException();
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            String hint = System.getenv("SSH_ASKPASS_PROMPT");
            output.writeUTF(hint == null ? "" : hint); output.writeUTF(args.length == 0 ? "" : args[0]); output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            int count = input.readInt();
            if (count >= 0 && count <= 8192) {
                byte[] answer = new byte[count];
                try { input.readFully(answer); System.out.write(answer); System.out.write('\n'); System.out.flush(); status = 0; }
                finally { Arrays.fill(answer, (byte) 0); }
            }
        } catch (Exception ignored) { /* Never print exception data or a secret. */ }
        System.exit(status);
    }
}
