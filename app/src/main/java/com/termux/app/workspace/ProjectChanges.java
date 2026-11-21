package com.termux.app.workspace;
import java.util.*;import java.nio.charset.StandardCharsets;import java.io.*;
/** Read-only Git changes and guarded initialization of a new chat's project. */
final class ProjectChanges {
 interface Reader{byte[] read(String script,int max)throws Exception;}
 static final String GIT="jobs_git";
 private static final String PATH_GIT = "jobs_git_executable=; "
     + "jobs_git_candidate=$(command -v git 2>/dev/null) || jobs_git_candidate=; "
     + "case \"$jobs_git_candidate\" in /*) "
     + "if [ -f \"$jobs_git_candidate\" ] && [ -x \"$jobs_git_candidate\" ]; "
     + "then jobs_git_executable=$jobs_git_candidate; fi;; esac; ";

 static String gitSelection(String binary, String sha256) {
  if (binary == null || !binary.startsWith("/") || sha256 == null || !sha256.matches("[0-9a-f]{64}"))
   return PATH_GIT;
  return PATH_GIT
      + "if [ -z \"$jobs_git_executable\" ]; then "
      + "jobs_git_candidate=" + SessionShell.quote(binary) + "; "
      + "if [ -f \"$jobs_git_candidate\" ] && [ -x \"$jobs_git_candidate\" ] "
      + "&& [ ! -L \"$jobs_git_candidate\" ]; then "
      + "jobs_git_digest=$(command sha256sum 2>/dev/null < \"$jobs_git_candidate\") || jobs_git_digest=; "
      + "if [ \"${jobs_git_digest%% *}\" = " + SessionShell.quote(sha256) + " ]; "
      + "then jobs_git_executable=$jobs_git_candidate; fi; fi; fi; ";
 }

 static String initializeScript(String selection) {
  return "( "
      + "jobs_git_init_failed(){ printf '%s\\n' 'Termux Jobs: could not initialize Git in the selected project.' >&2; }; "
      + "unset CDPATH; "
      + "[ -n \"${HOME:-}\" ] && jobs_git_cwd=$(pwd -P 2>/dev/null && printf .) "
      + "&& jobs_git_home=$(cd -P -- \"$HOME\" 2>/dev/null && pwd -P 2>/dev/null && printf .) "
      + "|| { jobs_git_init_failed; exit 0; }; "
      + "jobs_git_cwd=${jobs_git_cwd%??}; jobs_git_home=${jobs_git_home%??}; "
      + "case \"$jobs_git_cwd\" in ''|/) exit 0;; esac; "
      + "case \"$jobs_git_home\" in \"$jobs_git_cwd\"|\"$jobs_git_cwd\"/*) exit 0;; esac; "
      + "jobs_git_scope_free(){ jobs_git_parent=$jobs_git_cwd; "
      + "while :; do "
      + "if [ -e \"${jobs_git_parent%/}/.git\" ] || [ -L \"${jobs_git_parent%/}/.git\" ] "
      + "|| { [ -e \"${jobs_git_parent%/}/HEAD\" ] && [ -d \"${jobs_git_parent%/}/objects\" ]; }; "
      + "then return 1; fi; "
      + "[ \"$jobs_git_parent\" != / ] || break; "
      + "jobs_git_parent=${jobs_git_parent%/*}; jobs_git_parent=${jobs_git_parent:-/}; "
      + "done; }; jobs_git_scope_free || exit 0; "
      + selection
      + "if [ -z \"${jobs_git_executable:-}\" ]; then "
      + "printf '%s\\n' 'Termux Jobs: Git is unavailable in the selected environment.' >&2; exit 0; fi; "
      + "unset GIT_DIR GIT_WORK_TREE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY "
      + "GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_INDEX_FILE GIT_CONFIG GIT_CONFIG_PARAMETERS "
      + "GIT_DEFAULT_HASH GIT_DEFAULT_REF_FORMAT GIT_TEMPLATE_DIR; "
      + "export GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_SYSTEM=/dev/null GIT_CONFIG_GLOBAL=/dev/null "
      + "GIT_CONFIG_COUNT=0 GIT_TERMINAL_PROMPT=0; "
      + "jobs_git_stage=$(command mktemp -d .jobs-git-init.XXXXXXXX 2>/dev/null) "
      + "|| { jobs_git_init_failed; exit 0; }; "
      + "trap 'command rm -rf -- \"$jobs_git_stage\" >/dev/null 2>&1' 0; trap 'exit 0' HUP INT TERM; "
      + "if ! command \"$jobs_git_executable\" -C \"$jobs_git_stage\" init --quiet --template= >/dev/null 2>&1 "
      + "|| [ ! -f \"$jobs_git_stage/.git/HEAD\" ] "
      + "|| [ \"$(command \"$jobs_git_executable\" config --no-includes --file "
      + "\"$jobs_git_stage/.git/config\" --get core.bare 2>/dev/null)\" != false ]; then "
      + "jobs_git_init_failed; exit 0; fi; "
      + "jobs_git_scope_free || exit 0; "
      + "command mv -T -n -- \"$jobs_git_stage/.git\" .git 2>/dev/null || jobs_git_init_failed; "
      + ")";
 }

 private static final String SAFE = "if [ \"${jobs_git_executable+x}\" != x ]; then " + PATH_GIT
     + "fi; export GIT_OPTIONAL_LOCKS=0 GIT_TERMINAL_PROMPT=0; set -f; "
     + "jobs_git(){ [ -n \"$jobs_git_executable\" ] || return 127; "
     + "for jobs_key in $(command \"$jobs_git_executable\" config --name-only --get-regexp "
     + "'^filter[.].*[.](clean|smudge|process|required)$'); do "
     + "case \"$jobs_key\" in *[!a-zA-Z0-9._-]*) return 125;; esac; "
     + "case \"$jobs_key\" in *.clean|*.smudge|*.process) set -- -c \"$jobs_key=\" \"$@\";; "
     + "*.required) set -- -c \"$jobs_key=false\" \"$@\";; esac; done; "
     + "command \"$jobs_git_executable\" --no-optional-locks --no-pager --literal-pathspecs "
     + "-c core.fsmonitor=false -c core.untrackedCache=false \"$@\"; }; ";
 static final class Entry{final String path,code;String original="";Entry(String p,String c){path=p;code=c;}}
 static final class Listing{String root="",head="",state="";final List<Entry> entries=new ArrayList<>();}
 static String listScript(String dir) {
  return SAFE + "[ -n \"$jobs_git_executable\" ] || { printf 'NO_GIT\\0'; exit 0; }; "
      + "cd -- " + ProjectFiles.target(dir) + " || exit 2; "
      + "root=$(" + GIT + " rev-parse --show-toplevel 2>/dev/null) || { printf 'NO_PROJECT\\0'; exit 0; }; "
      + "cd -- \"$root\" || exit 2; "
      + "if head=$(" + GIT + " rev-parse --verify HEAD 2>/dev/null); then "
      + "printf 'OK\\0%s\\0%s\\0' \"$root\" \"$head\"; "
      + "else printf 'NO_HEAD\\0%s\\0' \"$root\"; fi; "
      + GIT + " status --porcelain=v1 -z --untracked-files=all";
 }

 static Listing parse(byte[] raw) throws IOException {
  String[] fields = new String(raw, StandardCharsets.UTF_8).split("\0", -1);
  Listing listing = new Listing();
  listing.state = fields[0];
  if (fields.length > 1) listing.root = fields[1];
  int firstEntry;
  if (listing.state.equals("OK")) {
   if (fields.length < 4) throw new IOException("INVALID_GIT_STATUS");
   listing.head = fields[2];
   firstEntry = 3;
  } else if (listing.state.equals("NO_HEAD")) {
   if (fields.length < 3) throw new IOException("INVALID_GIT_STATUS");
   firstEntry = 2;
  } else {
   return listing;
  }
  for (int i = firstEntry; i < fields.length && !fields[i].isEmpty(); i++) {
   String row = fields[i];
   if (row.length() < 4 || row.charAt(2) != ' ') throw new IOException("INVALID_GIT_STATUS");
   Entry entry = new Entry(row.substring(3), row.substring(0, 2));
   if (entry.code.indexOf('R') >= 0 || entry.code.indexOf('C') >= 0) {
    if (++i >= fields.length || fields[i].isEmpty()) throw new IOException("INVALID_RENAME");
    entry.original = fields[i];
   }
   listing.entries.add(entry);
  }
  return listing;
 }

 private static String diffSetup(Listing listing, Entry entry, String path) {
  String setup = SAFE + "cd -- " + ProjectFiles.target(listing.root) + " || exit 2; ";
  if (entry.code.equals("??")) return setup + "test -e " + path + " || test -L " + path + " || exit 2; ";
  if (listing.state.equals("NO_HEAD"))
   return setup + "jobs_git_base=$(" + GIT + " hash-object -t tree --stdin </dev/null) || exit 2; ";
  return setup + "jobs_git_base=" + SessionShell.quote(listing.head) + "; ";
 }

 private static String diffRange(Entry entry, String path) {
  String files = path + (entry.original.isEmpty() ? "" : " " + SessionShell.quote(entry.original));
  return entry.code.equals("??") ? "--no-index -- /dev/null " + files : "\"$jobs_git_base\" -- " + files;
 }

 static String diffScript(Listing listing, Entry entry) {
  String path = SessionShell.quote(entry.path);
  String diff = GIT + " diff --no-ext-diff --no-textconv --no-renames";
  String range = diffRange(entry, path);
  return diffSetup(listing, entry, path)
      + "n=$(" + diff + " --numstat " + range + "); jobs_result=$?; [ \"$jobs_result\" -le 1 ] || exit 2; "
      + "case \"$n\" in -*) printf 'BINARY\\n'; exit 0;; esac; printf 'TEXT\\n'; "
      + diff + " --no-color --unified=3 " + range + "; result=$?; [ \"$result\" -le 1 ]";
 }
 static final class Metadata{long modified;Long added,removed;boolean binary;}
 static String metadataScript(Listing listing, Entry entry) {
  String path = SessionShell.quote(entry.path);
  return diffSetup(listing, entry, path)
      + "printf 'MTIME\\0'; if [ -e " + path + " ]; then stat -L -c '%Y' -- " + path
      + " 2>/dev/null || true; fi; printf '\\0DIFF\\0'; "
      + GIT + " diff --no-ext-diff --no-textconv --find-renames --numstat -z " + diffRange(entry, path)
      + "; jobs_meta_result=$?; [ \"$jobs_meta_result\" -le 1 ]";
 }
 static Metadata metadata(Reader r,Listing l,Entry e)throws Exception{String raw=new String(r.read(metadataScript(l,e),ProjectFiles.MAX_PREVIEW),StandardCharsets.UTF_8);String[] parts=raw.split("\0",-1);Metadata result=new Metadata();if(parts.length<4||!parts[0].equals("MTIME")||!parts[2].equals("DIFF"))throw new IOException("INVALID_GIT_METADATA");if(e.code.indexOf('D')<0)try{result.modified=Math.multiplyExact(Long.parseLong(parts[1].trim()),1000L);}catch(Exception ignored){}long added=0,removed=0;for(int n=3;n<parts.length;n++){if(parts[n].isEmpty())continue;String[] cols=parts[n].split("\t",3);if(cols.length<3)throw new IOException("INVALID_GIT_NUMSTAT");if(cols[2].isEmpty()){if(n+2>=parts.length)throw new IOException("INVALID_GIT_RENAME_STAT");n+=2;}if(cols[0].equals("-")&&cols[1].equals("-")){result.binary=true;continue;}try{added=Math.addExact(added,Long.parseLong(cols[0]));removed=Math.addExact(removed,Long.parseLong(cols[1]));}catch(Exception invalid){throw new IOException("INVALID_GIT_NUMSTAT");}}result.added=added;result.removed=removed;return result;}
 static Listing list(Reader r,String dir)throws Exception{return parse(r.read(listScript(dir),ProjectFiles.MAX_LIST));}
 static String diff(Reader r,Listing l,Entry e)throws Exception{return new String(r.read(diffScript(l,e),ProjectFiles.MAX_PREVIEW),StandardCharsets.UTF_8);}
}
