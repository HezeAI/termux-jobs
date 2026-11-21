package com.termux.app.workspace;
import org.junit.*;import static org.junit.Assert.*;import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Genuine git in a private artificial project; no authorization or working repository. */
public class ProjectChangesTest {
 static void write(Path p,String text)throws Exception{Files.write(p,text.getBytes(StandardCharsets.UTF_8));}
 Path dir;
 String read(String script)throws Exception{return read(script,null);}
 String read(String script,Path path)throws Exception{
  ProcessBuilder builder=new ProcessBuilder("/bin/bash","--noprofile","--norc","-c",script);
  if(path!=null)builder.environment().put("PATH",path.toString());
  builder.environment().remove("BASH_ENV");
  builder.environment().put("GIT_CONFIG_NOSYSTEM","1");
  builder.environment().put("GIT_CONFIG_GLOBAL","/dev/null");
  Process p=builder.start();
  byte[] out=p.getInputStream().readAllBytes();
  byte[] err=p.getErrorStream().readAllBytes();
  assertEquals(new String(err,StandardCharsets.UTF_8),0,p.waitFor());
  return new String(out,StandardCharsets.UTF_8);
 }
 @Before public void setup()throws Exception{dir=Files.createTempDirectory(Path.of(System.getProperty("java.io.tmpdir")),"git-panel-");read("git init -q "+SessionShell.quote(dir.toString()));read("git -C "+SessionShell.quote(dir.toString())+" config user.email fixture@example.invalid; git -C "+SessionShell.quote(dir.toString())+" config user.name fixture");for(String f:new String[]{"modified.txt","deleted.txt","staged.txt","binary.bin"})write(dir.resolve(f),f+"\nbefore\n");read("git -C "+SessionShell.quote(dir.toString())+" add . && git -C "+SessionShell.quote(dir.toString())+" commit -qm initial");}
 ProjectChanges.Listing list()throws Exception{return ProjectChanges.parse(read(ProjectChanges.listScript(dir.toString())).getBytes(StandardCharsets.UTF_8));}
 String diff(ProjectChanges.Listing l,String name)throws Exception{ProjectChanges.Entry e=l.entries.stream().filter(v->v.path.equals(name)).findFirst().orElseThrow();return read(ProjectChanges.diffScript(l,e));}
 @Test public void stagedUnstagedNewDeletedBinaryAndLiteralPathsAreReadOnly()throws Exception{write(dir.resolve("modified.txt"),"after\n");Files.delete(dir.resolve("deleted.txt"));write(dir.resolve("staged.txt"),"staged content\n");read("git -C "+SessionShell.quote(dir.toString())+" add staged.txt");write(dir.resolve("staged.txt"),"staged and unstaged\n");Files.write(dir.resolve("binary.bin"),new byte[]{0,1,2,0});String weird="new $(touch SHOULD_NOT_EXIST) ' file.txt";write(dir.resolve(weird),"new text\n");byte[] index=Files.readAllBytes(dir.resolve(".git/index"));ProjectChanges.Listing l=list();assertEquals("OK",l.state);assertEquals(dir.toRealPath().toString(),l.root);assertEquals(5,l.entries.size());assertTrue(diff(l,"modified.txt").contains("+after"));assertTrue(diff(l,"deleted.txt").contains("-before"));assertTrue(diff(l,"staged.txt").contains("+staged and unstaged"));assertTrue(diff(l,weird).contains("+new text"));assertTrue(diff(l,"binary.bin").startsWith("BINARY"));assertFalse(Files.exists(dir.resolve("SHOULD_NOT_EXIST")));assertArrayEquals(index,Files.readAllBytes(dir.resolve(".git/index")));}
 @Test public void externalFiltersTextconvAndFsmonitorNeverRun()throws Exception{write(dir.resolve(".gitattributes"),"*.txt filter=spy diff=spy\n");String prefix="git -C "+SessionShell.quote(dir.toString())+" config ";for(String key:new String[]{"filter.spy.clean","filter.spy.process","diff.spy.textconv","diff.external","core.fsmonitor"})read(prefix+key+" "+SessionShell.quote("touch "+dir.resolve("MODEL_CALLED")));read(prefix+"filter.spy.required true");write(dir.resolve("modified.txt"),"after\n");ProjectChanges.Listing l=list();assertTrue(diff(l,"modified.txt").contains("+after"));assertEquals(Long.valueOf(1),metadata(l,"modified.txt").added);assertFalse(Files.exists(dir.resolve("MODEL_CALLED")));}
 @Test public void noGitProjectAndNoHeadAreExplicit()throws Exception{Path empty=Files.createTempDirectory(dir,"empty");String out=read(ProjectChanges.listScript("/"));assertTrue(out.startsWith("NO_PROJECT"));Path repo=Files.createTempDirectory(dir,"unborn");read("git init -q "+SessionShell.quote(repo.toString()));assertTrue(read(ProjectChanges.listScript(repo.toString())).startsWith("NO_HEAD"));}
 ProjectChanges.Metadata metadata(ProjectChanges.Listing l,String name)throws Exception{ProjectChanges.Entry entry=l.entries.stream().filter(v->v.path.equals(name)).findFirst().orElseThrow();return ProjectChanges.metadata((script,max)->read(script).getBytes(StandardCharsets.UTF_8),l,entry);}
 @Test public void metadataUsesWorkingFileTimeRealNumstatAndNoInventedDeletionDate()throws Exception{write(dir.resolve("modified.txt"),"after\n");long known=1700000000000L;Files.setLastModifiedTime(dir.resolve("modified.txt"),java.nio.file.attribute.FileTime.fromMillis(known));Files.delete(dir.resolve("deleted.txt"));Files.write(dir.resolve("new.bin"),new byte[]{0,1,2,0});read("git -C "+SessionShell.quote(dir.toString())+" mv staged.txt 'renamed spaced.txt'");ProjectChanges.Listing l=list();ProjectChanges.Metadata changed=metadata(l,"modified.txt");assertEquals(known,changed.modified);assertEquals(Long.valueOf(1),changed.added);assertEquals(Long.valueOf(2),changed.removed);ProjectChanges.Metadata deleted=metadata(l,"deleted.txt");assertEquals(0,deleted.modified);assertEquals(Long.valueOf(0),deleted.added);assertEquals(Long.valueOf(2),deleted.removed);assertTrue(metadata(l,"new.bin").binary);ProjectChanges.Metadata renamed=metadata(l,"renamed spaced.txt");assertEquals(Long.valueOf(0),renamed.added);assertEquals(Long.valueOf(0),renamed.removed);}

 @Test public void claudeUsesPinnedGitForChangesInTheSelectedRepository()throws Exception{
  WorkspaceStore.Session profile=ubuntuAiProfile();
  profile.claudeAgent=true;
  assertTrue(ClaudeCode.valid(profile));
  checkSelectedRepository(profile);
 }

 @Test public void antigravityUsesPinnedGitForChangesInTheSelectedRepository()throws Exception{
  WorkspaceStore.Session profile=ubuntuAiProfile();
  profile.geminiAgent=true;
  profile.geminiAuth=GeminiCli.ANTIGRAVITY;
  assertTrue(GeminiCli.valid(profile));
  checkSelectedRepository(profile);
 }

 @Test public void pinnedGitRequiresMatchingDigestAndUbuntuAi()throws Exception{
  GitRoute route=new GitRoute();
  WorkspaceStore.Session profile=ubuntuAiProfile();
  profile.claudeAgent=true;
  String script=ProjectChanges.listScript(dir.toRealPath().toString());
  String wrongHash=(route.hash.startsWith("0")?"1":"0")+route.hash.substring(1);
  assertEquals("NO_GIT",ProjectChanges.parse(route.read(profile,script,wrongHash)).state);
  profile.codexEnvironment="ubuntu";
  assertEquals("NO_GIT",ProjectChanges.parse(route.read(profile,script,route.hash)).state);

  Files.createSymbolicLink(route.path.resolve("git"),route.pinnedGit);
  profile.codexEnvironment="ubuntu-ai";
  assertEquals("OK",ProjectChanges.parse(route.read(profile,script,wrongHash)).state);
 }

 @Test public void newlyInitializedRepositoryShowsUntrackedFilesWithoutFirstCommit()throws Exception{
  GitRoute route=new GitRoute();
  WorkspaceStore.Session profile=ubuntuAiProfile();
  profile.geminiAgent=true;
  profile.geminiAuth=GeminiCli.ANTIGRAVITY;
  Path repository=Files.createDirectory(dir.resolve("новый репозиторий "));
  read("git init -q "+SessionShell.quote(repository.toString()));
  write(repository.resolve("first.txt"),"initial proposal\n");
  ProjectChanges.Reader reader=(script,max)->route.read(profile,script,route.hash);

  ProjectChanges.Listing listing=ProjectChanges.list(reader,repository.toRealPath().toString());
  assertEquals("NO_HEAD",listing.state);
  assertEquals(repository.toRealPath().toString(),listing.root);
  assertEquals(1,listing.entries.size());
  ProjectChanges.Entry entry=listing.entries.get(0);
  assertEquals("first.txt",entry.path);
  assertEquals("??",entry.code);
  assertEquals(Long.valueOf(1),ProjectChanges.metadata(reader,listing,entry).added);
  assertTrue(ProjectChanges.diff(reader,listing,entry).contains("+initial proposal"));
 }

 @Test public void initializationCreatesOnlyTheSelectedRepositoryWithoutACommit()throws Exception{
  GitRoute route=new GitRoute();
  Path home=Files.createTempDirectory(dir.toRealPath().getParent(),"git-init-home-");
  Path projects=Files.createDirectory(home.resolve("projects"));
  Path selected=Files.createDirectory(projects.resolve("проект ' "));
  Path sibling=Files.createDirectory(projects.resolve("other project"));
  write(selected.resolve("source.txt"),"project contents\n");

  initializeProject(home,home,route);
  initializeProject(selected,home,route);

  assertTrue(Files.isDirectory(selected.resolve(".git")));
  assertEquals("0",read("git -C "+SessionShell.quote(selected.toString())+" rev-list --all --count").trim());
  assertTrue(Files.notExists(selected.resolve(".git/index")));
  assertEquals("project contents\n",new String(Files.readAllBytes(selected.resolve("source.txt")),StandardCharsets.UTF_8));
  assertTrue(Files.notExists(home.resolve(".git")));
  assertTrue(Files.notExists(projects.resolve(".git")));
  assertTrue(Files.notExists(sibling.resolve(".git")));
  String git="git -C "+SessionShell.quote(selected.toString());
  read(git+" add source.txt && "+git
    +" -c user.name=Fixture -c user.email=fixture@example.invalid commit -qm 'First commit'");
  assertEquals("First commit",read(git+" log -1 --format=%s").trim());
 }

 @Test public void initializationPreservesAnExistingRepositoryAndItsSubdirectories()throws Exception{
  GitRoute route=new GitRoute();
  Path nested=Files.createDirectory(dir.resolve("existing worktree subdirectory"));
  Path config=dir.resolve(".git/config"),index=dir.resolve(".git/index");
  byte[] originalConfig=Files.readAllBytes(config),originalIndex=Files.readAllBytes(index);
  Path home=dir.toRealPath().getParent();

  initializeProject(dir,home,route);
  initializeProject(nested,home,route);

  assertArrayEquals(originalConfig,Files.readAllBytes(config));
  assertArrayEquals(originalIndex,Files.readAllBytes(index));
  assertTrue(Files.notExists(nested.resolve(".git")));
 }

 @Test public void failedInitializationLeavesNoPartialRepositoryAndCanRetry()throws Exception{
  GitRoute route=new GitRoute();
  Path home=Files.createTempDirectory(dir.toRealPath().getParent(),"git-retry-home-");
  Path selected=Files.createDirectory(home.resolve("project"));
  Path failedBin=Files.createDirectory(home.resolve("failed-bin"));
  Path failedGit=failedBin.resolve("git");
  write(failedGit,"#!/bin/sh\nprintf called > "+SessionShell.quote(failedBin.resolve("invoked").toString())+"\nexit 3\n");
  assertTrue(failedGit.toFile().setExecutable(true));
  String selection="export PATH="+SessionShell.quote(failedBin.toString())+"\":$PATH\"; "+ProjectChanges.gitSelection("","");
  initializeProject(selected,home,selection,route.path);
  assertTrue(Files.exists(failedBin.resolve("invoked")));
  try(java.util.stream.Stream<Path> files=Files.list(selected)){assertFalse(files.findAny().isPresent());}
  initializeProject(selected,home,route);
  assertTrue(Files.isDirectory(selected.resolve(".git")));
 }

 @Test public void newClaudeAndAntigravityStartupInitializeBeforeProviderExecution()throws Exception{
  for(boolean claude:new boolean[]{true,false}){
   WorkspaceStore.Session profile=ubuntuAiProfile();
   profile.claudeAgent=claude;
   profile.geminiAgent=!claude;
   if(!claude)profile.geminiAuth=GeminiCli.ANTIGRAVITY;
   profile.initializeProjectGit=ProjectFiles.startsNewProjectGit(profile);
   assertTrue(profile.initializeProjectGit);
   String executable=claude?"exec \"$JOBS_CLAUDE_NODE\"":"exec \"$JOBS_AGY_NODE\"";
   String marker="jobs_git_init_failed(){";
   File files=dir.resolve(claude?"claude-session":"antigravity-session").toFile();
   SessionShell.prepare(files,profile);
   Path guestRc=SessionShell.directory(files,profile.id).toPath().resolve("ubuntu-bashrc");
   String startup=new String(Files.readAllBytes(guestRc),StandardCharsets.UTF_8);
   assertTrue(startup.contains(marker));
   assertTrue(startup.indexOf(executable)>startup.indexOf(marker));

   profile.claudeChat="id";
   profile.geminiChat="id";
   profile.initializeProjectGit=ProjectFiles.startsNewProjectGit(profile);
   assertFalse(profile.initializeProjectGit);
   SessionShell.prepare(files,profile);
   String resumed=new String(Files.readAllBytes(guestRc),StandardCharsets.UTF_8);
   assertTrue(resumed.contains(executable));
   assertFalse(resumed.contains(marker));
  }
 }

 private void initializeProject(Path selected,Path home,GitRoute route)throws Exception{
  String selection=ProjectChanges.gitSelection(route.pinnedGit.toString(),route.hash);
  initializeProject(selected,home,selection,route.path);
 }

 private void initializeProject(Path selected,Path home,String selection,Path path)throws Exception{
  // The test directory stays inside the checkout. Hide that single outside
  // .git boundary to model a guest root while preserving checks within the fixture.
  String outerRepo=read("git -C "+SessionShell.quote(home.toString())
    +" rev-parse --show-toplevel 2>/dev/null || :").trim();
  String isolated=outerRepo.isEmpty()?"":"function [ { "
    +"if [[ ( $1 == -e || $1 == -L ) && $2 == "
    +SessionShell.quote(outerRepo+"/.git")+" ]]; then return 1; fi; builtin [ \"$@\"; }; ";
  read(isolated+"export HOME="+SessionShell.quote(home.toString())+"; cd -- "
    +SessionShell.quote(selected.toString())+" && "+ProjectChanges.initializeScript(selection),path);
 }

 private WorkspaceStore.Session ubuntuAiProfile(){
  WorkspaceStore.Session profile=new WorkspaceStore.Session();
  profile.environment="ubuntu";
  profile.codexEnvironment="ubuntu-ai";
  profile.directory=dir.resolve("unrelated-startup-directory").toString();
  return profile;
 }

 private void checkSelectedRepository(WorkspaceStore.Session profile)throws Exception{
  GitRoute route=new GitRoute();
  Path selected=Files.createDirectory(dir.resolve("selected ' folder"));
  write(dir.resolve("modified.txt"),"after\n");
  Path index=dir.resolve(".git/index");
  byte[] originalIndex=Files.readAllBytes(index);
  ProjectChanges.Reader reader=(script,max)->route.read(profile,script,route.hash);

  assertEquals("proot:ubuntu-ai",ProjectFiles.environment(profile));
  ProjectChanges.Listing listing=ProjectChanges.list(reader,selected.toRealPath().toString());
  assertEquals("OK",listing.state);
  assertEquals(dir.toRealPath().toString(),listing.root);
  assertEquals(1,listing.entries.size());
  ProjectChanges.Entry entry=listing.entries.get(0);
  assertEquals("modified.txt",entry.path);
  assertEquals(" M",entry.code);
  assertEquals(Long.valueOf(1),ProjectChanges.metadata(reader,listing,entry).added);
  assertTrue(ProjectChanges.diff(reader,listing,entry).contains("+after"));
  assertArrayEquals(originalIndex,Files.readAllBytes(index));
 }

 private final class GitRoute {
  final Path pinnedGit,path;
  final String hash;

  GitRoute()throws Exception{
   Path realGit=Path.of(ProjectChangesTest.this.read("command -v git").trim()).toRealPath();
   pinnedGit=dir.toRealPath().resolve(".tools/git-local/usr/bin/git");
   Files.createDirectories(pinnedGit.getParent());
   Files.copy(realGit,pinnedGit,StandardCopyOption.COPY_ATTRIBUTES);
   Files.createDirectories(dir.resolve(".git/info"));
   write(dir.resolve(".git/info/exclude"),"/.tools/\n");
   hash=ProjectChangesTest.this.read("sha256sum -- "+SessionShell.quote(pinnedGit.toString())).split(" ",2)[0];

   path=Files.createTempDirectory(dir.toRealPath().getParent(),"git-route-path-");
   for(String tool:new String[]{"mktemp","mv","rm","sha256sum","stat"}){
    Files.createSymbolicLink(path.resolve(tool),Path.of(ProjectChangesTest.this.read("command -v "+tool).trim()));
   }
   Path proot=path.resolve("proot-distro");
   write(proot,"#!/bin/sh\n"
    +"case \"$1\" in login|run) ;; *) exit 82;; esac\n"
    +"shift\n"
    +"[ \"$1\" != --isolated ] || shift\n"
    +"while [ \"$1\" = --bind ]; do shift 2; done\n"
    +"case \"$1:$3\" in ubuntu-ai:/bin/sh|ubuntu:/bin/bash) ;; *) exit 83;; esac\n"
    +"[ \"$2\" = -- ] || exit 84\nshift 2\nexec \"$@\"\n");
   assertTrue(proot.toFile().setExecutable(true));
  }

  byte[] read(WorkspaceStore.Session profile,String script,String digest)throws Exception{
   String command=ProjectFiles.gitCommand(profile,script,pinnedGit.toString(),digest);
   return ProjectChangesTest.this.read(command,path).getBytes(StandardCharsets.UTF_8);
  }
 }
}
