package com.termux.app.workspace;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Trusted, APK-bundled guidance only; never loads project-controlled instructions. */
final class LocalAiInstructions {
    static final String ASSET = "local-ai-agent-instructions.md";
    private static volatile String bundled;
    static String load(Context context) throws IOException {
        String value = bundled;
        if (value != null) return value;
        synchronized (LocalAiInstructions.class) {
            if (bundled != null) return bundled;
            try (InputStream in = context.getAssets().open(ASSET);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048]; int count;
                while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 16384) throw new IOException("LOCAL_AI_INSTRUCTIONS_TOO_LARGE");
                    out.write(buffer, 0, count);
                }
                value = new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
                if (value.isEmpty()) throw new IOException("LOCAL_AI_INSTRUCTIONS_EMPTY");
                bundled = value;
                return value;
            }
        }
    }
}
