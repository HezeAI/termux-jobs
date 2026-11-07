package com.termux.app.workspace;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Removal migration for the retired on-device model feature, not a workspace reset. */
final class RetiredModelData {
    private RetiredModelData() {}

    static void clean(Context context) throws IOException {
        removeOwnedDirectory(context.getFilesDir());
        removeOwnedDirectory(context.getCacheDir());
        removeDictation(context);
        context.deleteSharedPreferences("local-ai-diagnostics");
    }

    private static final String DICTATION_SHA="961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4";
    private static void removeDictation(Context context)throws IOException{
        try(java.io.InputStream in=context.getAssets().open("retired-dictation-files.json")){
            java.io.ByteArrayOutputStream text=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1)text.write(block,0,n);
            org.json.JSONObject json=new org.json.JSONObject(text.toString("UTF-8"));java.util.Map<String,String> expected=new java.util.HashMap<>();java.util.Iterator<String> keys=json.keys();while(keys.hasNext()){String key=keys.next();expected.put(key,json.getString(key));}
            removeVerifiedDictation(context.getCacheDir(),expected);
        }catch(org.json.JSONException invalid){throw new IOException("Invalid retired-data manifest",invalid);}
    }
    static void removeVerifiedDictation(File cache,java.util.Map<String,String> model)throws IOException{
        Path parent=cache.getCanonicalFile().toPath().resolve("dictation-models");
        Path root=parent.resolve("ru-0.22-"+DICTATION_SHA.substring(0,12));
        if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(root)||!Files.isDirectory(root,java.nio.file.LinkOption.NOFOLLOW_LINKS))return;
        java.util.Map<String,String> expected=new java.util.HashMap<>(model);
        expected.put("ready.sha256",sha(DICTATION_SHA.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        java.util.Set<String> allowedDirectories=new java.util.HashSet<>();allowedDirectories.add("");for(String name:expected.keySet()){Path part=java.nio.file.Paths.get(name).getParent();while(part!=null){allowedDirectories.add(part.toString());part=part.getParent();}}
        java.util.List<Path> owned=new java.util.ArrayList<>();java.util.Set<String> found=new java.util.HashSet<>();
        try(java.util.stream.Stream<Path> paths=Files.walk(root)){
            for(Path p:(Iterable<Path>)paths::iterator){
                if(Files.isSymbolicLink(p))return;
                if(Files.isDirectory(p,java.nio.file.LinkOption.NOFOLLOW_LINKS)){if(!allowedDirectories.contains(root.relativize(p).toString()))return;owned.add(p);continue;}
                String key=root.relativize(p).toString();if(!expected.containsKey(key)||!Files.isRegularFile(p,java.nio.file.LinkOption.NOFOLLOW_LINKS))return;
                try(java.io.InputStream in=Files.newInputStream(p,java.nio.file.LinkOption.NOFOLLOW_LINKS)){
                    java.security.MessageDigest digest=digest();byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);if(!expected.get(key).equals(hex(digest.digest())))return;
                }
                found.add(key);owned.add(p);
            }
        }
        if(!found.equals(expected.keySet()))return; // Unknown, partial or altered files are preserved.
        owned.sort(java.util.Comparator.reverseOrder());for(Path p:owned)Files.delete(p);
        try(java.nio.file.DirectoryStream<Path> entries=Files.newDirectoryStream(parent)){if(entries.iterator().hasNext())return;}
        Files.delete(parent);
    }
    private static java.security.MessageDigest digest(){try{return java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String sha(byte[] b){return hex(digest().digest(b));}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte n:b)s.append(String.format(java.util.Locale.ROOT,"%02x",n&255));return s.toString();}

    static void removeOwnedDirectory(File appDirectory) throws IOException {
        // Fixed child only: no profile-supplied paths, HOME, PREFIX or proot roots.
        Path child = appDirectory.getCanonicalFile().toPath().resolve("local-ai");
        if (!Files.exists(child, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        // Default walk never follows symbolic links, including the top-level path.
        Files.walkFileTree(child, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
