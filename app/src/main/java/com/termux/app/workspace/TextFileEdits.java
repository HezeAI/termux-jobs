package com.termux.app.workspace;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;

/** Bounded UTF-8 snapshots and same-directory atomic saves; never treats a read failure as empty text. */
final class TextFileEdits {
    static final int MAX_BYTES=256*1024;
    static final class Snapshot {
        final String path,revision,text;
        Snapshot(String path,String revision,String text){this.path=path;this.revision=revision;this.text=text;}
    }
    private static String revisionFunction(){return "revision() { local hash metadata; hash=$(sha256sum < \"$file\") || return; hash=${hash%% *}; metadata=$(stat -c '%d:%i:%u:%g:%a:%s:%y:%z:%h' -- \"$file\") || return; printf '%s|%s' \"$hash\" \"$metadata\"; }; ";}
    static String readScript(String path){
        return "set -e; export LC_ALL=C; file="+ProjectFiles.target(path)+"; "
            +"test -f \"$file\"; test ! -L \"$file\"; test -r \"$file\"; "
            +"test \"$(stat -c %h -- \"$file\")\" = 1; test \"$(stat -c %s -- \"$file\")\" -le "+MAX_BYTES+"; "
            +revisionFunction()+"before=$(revision); printf '%s\\0' \"$before\"; cat -- \"$file\"; test \"$before\" = \"$(revision)\";";
    }
    static Snapshot parse(String path,byte[] bytes)throws IOException{
        int split=-1;for(int i=0;i<bytes.length;i++)if(bytes[i]==0){split=i;break;}
        if(split<65||split>512||bytes.length-split-1>MAX_BYTES)throw error();
        String revision=new String(bytes,0,split,StandardCharsets.UTF_8);
        if(!revision.substring(0,64).matches("[0-9a-f]{64}")||revision.charAt(64)!='|')throw error();
        byte[] content=java.util.Arrays.copyOfRange(bytes,split+1,bytes.length);
        for(byte b:content)if(b==0)throw error();
        try {
            if(!digest(content).equals(revision.substring(0,64)))throw error();
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
            return new Snapshot(path,revision,text);
        }catch(CharacterCodingException exception){throw error();}
    }
    static byte[] encode(String text)throws IOException{
        if(text.indexOf('\0')>=0)throw error();
        try{ByteBuffer encoded=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text));byte[] bytes=new byte[encoded.remaining()];encoded.get(bytes);if(bytes.length>MAX_BYTES)throw error();return bytes;}catch(CharacterCodingException e){throw error();}
    }
    private static IOException error(){return new IOException(WorkspaceText.tr("Редактор поддерживает текст UTF-8 до 256 КБ. Бинарные файлы и повреждённый текст не редактируются."));}
    private static String digest(byte[] bytes){try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder result=new StringBuilder();for(byte b:hash)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    static String saveScript(Snapshot original){
        return "set -e; export LC_ALL=C; file="+ProjectFiles.target(original.path)+"; "
            +"directory=$(dirname -- \"$file\"); tmp=$(mktemp -- \"$directory/.termux-jobs-edit.XXXXXXXX\"); trap 'rm -f -- \"$tmp\"' EXIT; "
            +"cat > \"$tmp\"; test \"$(stat -c %s -- \"$tmp\")\" -le "+MAX_BYTES+"; "
            +"if [ ! -f \"$file\" ] || [ -L \"$file\" ] || [ \"$(stat -c %h -- \"$file\")\" != 1 ]; then printf CONFLICT; exit 0; fi; "
            +revisionFunction()+"expected="+SessionShell.quote(original.revision)+"; if [ \"$(revision)\" != \"$expected\" ]; then printf CONFLICT; exit 0; fi; "
            // Copy metadata without exposing the new content as shell text; preserve mode/uid/gid.
            +"metadata=$(stat -c '%u:%g:%a' -- \"$file\"); meta=$(mktemp -- \"$directory/.termux-jobs-edit.XXXXXXXX\"); trap 'rm -f -- \"$tmp\" \"$meta\"' EXIT; cp -p -- \"$file\" \"$meta\"; "
            +"cat -- \"$tmp\" > \"$meta\"; test \"$(stat -c '%u:%g:%a' -- \"$meta\")\" = \"$metadata\"; "
            +"if [ -L \"$file\" ] || [ \"$(revision)\" != \"$expected\" ]; then printf CONFLICT; exit 0; fi; "
            +"mv -fT -- \"$meta\" \"$file\"; printf SAVED;";
    }
}
