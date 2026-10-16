package com.termux.app.workspace;

import android.app.Activity;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Handler;
import android.os.Looper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** One password attempt, same-UID socket, no plaintext on disk, argv or environment. */
public final class SshPasswordBroker implements AutoCloseable {
    public final String address = "termux-jobs-ssh-" + UUID.randomUUID();
    private final LocalServerSocket server;
    private final WeakReference<Activity> activity;
    private char[] password;
    private final String keyPath;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable expiry = this::close;
    public SshPasswordBroker(Activity activity, char[] password) throws IOException {
        this(activity,password,null);
    }
    public SshPasswordBroker(Activity activity,char[] password,String keyPath)throws IOException {
        this.keyPath=keyPath;
        this.activity = new WeakReference<>(activity);
        server = new LocalServerSocket(address); this.password = password.clone();
        main.postDelayed(expiry, 120000);
        Thread worker = new Thread(this::serve, "ssh-password-once"); worker.setDaemon(true); worker.start();
    }
    static boolean isPasswordPrompt(String hint, String prompt) {
        return hint.isEmpty() && prompt.trim().endsWith("password:");
    }
    static boolean isKeyPassphrasePrompt(String keyPath,String hint,String prompt){
        if(keyPath==null||!hint.isEmpty())return false;
        // OpenSSH formats this prompt with %.100s (bytes, not Java characters).
        byte[] path=keyPath.getBytes(StandardCharsets.UTF_8);
        String shown=new String(path,0,Math.min(100,path.length),StandardCharsets.UTF_8);
        return prompt.trim().equals("Enter passphrase for key '"+shown+"':");
    }
    static boolean isHostPrompt(String hint, String prompt) {
        return hint.equals("confirm") || prompt.contains("Are you sure you want to continue connecting");
    }
    private boolean confirm(String prompt) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1); AtomicBoolean accepted = new AtomicBoolean();
        main.post(() -> {
            Activity current = activity.get();
            if (closed.get() || current == null || current.isFinishing() || current.isDestroyed()) { done.countDown(); return; }
            android.app.AlertDialog dialog = WorkspaceDialogs.builder(current).setTitle(WorkspaceText.tr("Проверка SSH-сервера"))
                .setMessage(prompt).setPositiveButton(WorkspaceText.tr("Доверять"), (d, w) -> { accepted.set(true); done.countDown(); })
                .setNegativeButton(WorkspaceText.tr("Отмена"), (d, w) -> done.countDown()).create();
            dialog.setOnDismissListener(d -> done.countDown()); dialog.show();
            main.postDelayed(() -> { if (dialog.isShowing()) dialog.dismiss(); }, 90000);
        });
        return done.await(95, TimeUnit.SECONDS) && accepted.get() && !closed.get();
    }
    private void serve() {
        try {
            while (!closed.get()) {
                try (LocalSocket socket = server.accept()) {
                    socket.setSoTimeout(10000);
                    if (socket.getPeerCredentials().getUid() != android.os.Process.myUid()) continue;
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    String hint = input.readUTF(), prompt = input.readUTF();
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    if (isHostPrompt(hint, prompt)) {
                        byte[] answer = (confirm(prompt) ? "yes" : "no").getBytes(StandardCharsets.UTF_8);
                        output.writeInt(answer.length); output.write(answer); output.flush();
                        if (answer[0] == 'n') break;
                    } else if (keyPath==null ? isPasswordPrompt(hint,prompt) : isKeyPassphrasePrompt(keyPath,hint,prompt)) {
                        byte[] answer;
                        synchronized (this) {
                            if (password == null || closed.get()) { output.writeInt(-1); output.flush(); break; }
                            ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)); answer = new byte[buffer.remaining()]; buffer.get(answer);
                            if (buffer.hasArray()) Arrays.fill(buffer.array(), (byte) 0);
                        }
                        try { output.writeInt(answer.length); output.write(answer); output.flush(); }
                        finally { Arrays.fill(answer, (byte) 0); }
                        break;
                    } else { output.writeInt(-1); output.flush(); break; }
                }
            }
        } catch (Exception ignored) { /* Fail closed, without logging authentication data. */ }
        finally { close(); }
    }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        main.removeCallbacks(expiry);
        synchronized (this) { if (password != null) { Arrays.fill(password, '\0'); password = null; } }
        try { server.close(); } catch (IOException ignored) {}
    }
}
