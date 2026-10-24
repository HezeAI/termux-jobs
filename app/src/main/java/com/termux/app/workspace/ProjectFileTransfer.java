package com.termux.app.workspace;

import java.util.*;

/** Quoted operations in one selected environment, with whole-batch preflight and no overwrite. */
final class ProjectFileTransfer {
    static final int MAX_SELECTED=50;
    interface Runner { void run(String script,java.io.OutputStream output,int max) throws Exception; }
    static void execute(List<ProjectFiles.Entry> entries,String folder,boolean move,boolean rename,Runner runner)throws Exception {
        ProjectFileReceipt receipt=new ProjectFileReceipt(entries.size());
        String script=script(entries,folder,move,rename,false,receipt);
        try {runner.run(script,receipt,ProjectFileReceipt.MAX_OUTPUT);receipt.requireComplete();}
        catch(Exception error){throw new ProjectFiles.TransferFailure(receipt.acknowledged(),error);}
    }
    static String script(List<ProjectFiles.Entry> entries,String folder,boolean move,boolean rename,boolean checkOnly){
        return script(entries,folder,move,rename,checkOnly,null);
    }
    static String script(List<ProjectFiles.Entry> entries,String folder,boolean move,boolean rename,boolean checkOnly,ProjectFileReceipt receipt){
        if(entries.isEmpty()||entries.size()>MAX_SELECTED)throw new IllegalArgumentException("INVALID_SELECTION");
        StringBuilder s=new StringBuilder("set -eu\numask 077\nexport LC_ALL=C\nto=$(cd -P -- "+ProjectFiles.target(folder)+" && pwd -P) || exit 21\n");
        Set<String> paths=new HashSet<>();
        for(int i=0;i<entries.size();i++){
            ProjectFiles.Entry e=entries.get(i);if(!e.path.startsWith("/")||e.name.isEmpty()||e.name.equals(".")||e.name.equals("..")||e.name.contains("/")||!paths.add(e.path))throw new IllegalArgumentException("INVALID_SELECTION");
            String parent=e.path.substring(0,e.path.lastIndexOf('/'));if(parent.isEmpty())parent="/";
            s.append("parent=$(cd -P -- ").append(ProjectFiles.target(parent)).append(" && pwd -P) || exit 21\n")
                .append("src").append(i).append("=\"${parent%/}/\"").append(SessionShell.quote(e.name)).append("\n")
                .append("src=\"$src").append(i).append("\"\n")
                .append("if ! [ -f \"$src\" ] && ! [ -d \"$src\" ] && ! [ -L \"$src\" ]; then exit 21; fi\n")
                .append("if [ -d \"$src\" ] && ! [ -L \"$src\" ]; then case \"$to/\" in \"$src/\"*) exit 22;; esac; fi\n");
            if(i>0){s.append("for previous in");for(int j=0;j<i;j++)s.append(" \"$src").append(j).append("\"");s.append("; do\ncase \"$src/\" in \"$previous/\"*) exit 22;; esac\ncase \"$previous/\" in \"$src/\"*) exit 22;; esac\ndone\n");}
            int dot=e.directory?-1:e.name.lastIndexOf('.');String stem=dot>0?e.name.substring(0,dot):e.name,suffix=dot>0?e.name.substring(dot):"";
            s.append("stem=").append(SessionShell.quote(stem)).append("\nsuffix=").append(SessionShell.quote(suffix)).append("\n");
            s.append("name=").append(SessionShell.quote(e.name)).append("\ndst=\"${to%/}/$name\"\nbase=$name\nn=1\nwhile :; do\ncollision=0\n[ ! -e \"$dst\" ] && [ ! -L \"$dst\" ] || collision=1\n");
            if(i>0){s.append("for previous in");for(int j=0;j<i;j++)s.append(" \"$dst").append(j).append("\"");s.append("; do [ \"$dst\" != \"$previous\" ] || collision=1; done\n");}
            s.append("[ \"$collision\" = 1 ] || break\n");
            if(!rename)s.append("exit 23\n");else s.append("n=$((n+1))\n[ \"$n\" -le 10000 ] || exit 23\ndst=\"${to%/}/$stem ($n)$suffix\"\n");
            s.append("done\ndst").append(i).append("=$dst\n");
        }
        if(checkOnly)return s.append("printf 'READY'\n").toString();
        if(receipt==null)receipt=new ProjectFileReceipt(entries.size());
        // GNU/Termux mv -T -n never merges into a directory or overwrites a raced-in target.
        for(int i=0;i<entries.size();i++){
            s.append("src=\"$src").append(i).append("\"\ndst=\"$dst").append(i).append("\"\n");
            if(move)s.append("mv -T -n -- \"$src\" \"$dst\" || exit 24\nif [ -e \"$src\" ] || [ -L \"$src\" ]; then exit 23; fi\n");
            else s.append("tmp=$(mktemp -d -- \"${to%/}/.termux-jobs-copy.XXXXXXXX\") || exit 24\ntrap 'rm -rf -- \"$tmp\"' EXIT\ncp -R -P -- \"$src\" \"$tmp/item\" || exit 24\nmv -T -n -- \"$tmp/item\" \"$dst\" || exit 24\nif [ -e \"$tmp/item\" ] || [ -L \"$tmp/item\" ]; then exit 23; fi\nrmdir -- \"$tmp\"\ntrap - EXIT\n");
            s.append("printf '%s\\n' ").append(SessionShell.quote(receipt.progress(i+1))).append("\n");
        }
        return s.append("printf '%s\\n' ").append(SessionShell.quote(receipt.completion())).append("\n").toString();
    }
}
