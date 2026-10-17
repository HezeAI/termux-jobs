package com.termux.app.workspace;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Archive metadata only. Does not install, execute, extract or decompile APK code. */
final class LocalAiApk {
    static String inspect(Context context,File archive,String source,java.util.concurrent.atomic.AtomicBoolean cancel)throws Exception{
        JSONObject result=new JSONObject().put("source",source).put("bytes",archive.length()).put("notice","Archive/package metadata only; no code execution/decompilation and no independent cryptographic signature verification");JSONArray entries=new JSONArray();Set<String> abis=new TreeSet<>();int count=0;
        try(ZipFile zip=new ZipFile(archive)){Enumeration<? extends ZipEntry> all=zip.entries();while(all.hasMoreElements()){LocalAiFiles.check(cancel);ZipEntry entry=all.nextElement();count++;if(count<=100)entries.put(new JSONObject().put("name",entry.getName()).put("size",entry.getSize()));if(entry.getName().startsWith("lib/")){String[] parts=entry.getName().split("/");if(parts.length>=3)abis.add(parts[1]);}if(count>100000)throw new IOException("ARCHIVE_TOO_COMPLEX: too many ZIP entries");}}
        result.put("entryCount",count).put("entries",entries).put("entriesTruncated",count>100).put("abis",new JSONArray(abis));int flags=PackageManager.GET_PERMISSIONS|(Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);LocalAiFiles.check(cancel);PackageInfo info=context.getPackageManager().getPackageArchiveInfo(archive.getPath(),flags);
        if(info==null)result.put("packageMetadata","Android could not parse package metadata");else{result.put("package",info.packageName).put("versionName",info.versionName).put("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);if(info.applicationInfo!=null)result.put("minSdk",info.applicationInfo.minSdkVersion).put("targetSdk",info.applicationInfo.targetSdkVersion);result.put("permissions",new JSONArray(info.requestedPermissions==null?Collections.emptyList():Arrays.asList(info.requestedPermissions)));Signature[] signatures=Build.VERSION.SDK_INT>=28?(info.signingInfo==null?null:info.signingInfo.getApkContentsSigners()):info.signatures;JSONArray certs=new JSONArray();if(signatures!=null)for(Signature signature:signatures){byte[] hash=MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());StringBuilder hex=new StringBuilder();for(byte value:hash)hex.append(String.format(Locale.ROOT,"%02x",value&255));certs.put(hex.toString());}result.put("certificateSHA256",certs);}return "exit=0\n"+result;
    }
}
