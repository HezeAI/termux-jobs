package com.termux.app.workspace;

import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class PackageCatalogTest {
    private byte[] data(String s){return s.getBytes(StandardCharsets.UTF_8);}
    @Test public void installedOnlyIncludesInstalledStatusAndPreservesArchitecture()throws Exception{
        List<PackageCatalog.Entry> entries=PackageCatalog.parse(data("ii \tbash\t5.2\nrc \told-package\t1\nii \tlibc6:arm64\t2.36\niU \tbroken\t1\n"),0);
        assertEquals(2,entries.size());assertEquals("bash",entries.get(0).name);assertEquals("libc6:arm64",entries.get(1).name);
    }
    @Test public void availableAndUpgradeMetadataIgnoreHeaderAndKeepVersions()throws Exception{
        List<PackageCatalog.Entry> entries=PackageCatalog.parse(data("Listing...\nbash/stable 5.2 arm64 [installed]\nhtop/stable 3.4 arm64\n"),1);
        assertEquals(2,entries.size());assertEquals("5.2",entries.get(0).version);
        assertEquals("htop",PackageCatalog.parse(data("Listing...\nhtop/stable 3.4 arm64 [upgradable from: 3.3]\n"),2).get(0).name);
        assertTrue(PackageCatalog.parse(data("Listing...\n"),2).isEmpty());
    }
    @Test public void injectionAndMalformedPackagesRejected()throws Exception{
        for(String name:new String[]{"-y","$(touch /tmp/oops)","bash;id","bash\nrm","a/b","a'bad"}){
            assertFalse(PackageCatalog.validName(name));try{PackageCatalog.operation("install",name);fail();}catch(IllegalArgumentException expected){}
        }
        try{PackageCatalog.parse(data("bad;id/stable 1 arm64\n"),1);fail();}catch(IOException expected){}
        try{PackageCatalog.parse(data("bash/stable\n"),1);fail();}catch(IOException expected){}
        try{PackageCatalog.operation("purge","bash");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void mutationsAreInteractiveAndNoDestructiveReset(){
        assertEquals("apt-get update",PackageCatalog.operation("refresh",null));
        assertEquals("apt-get install -- 'htop'",PackageCatalog.operation("install","htop"));
        assertTrue(PackageCatalog.operation("upgrade","bash").contains("--only-upgrade"));
        assertEquals("apt-get remove -- 'htop'",PackageCatalog.operation("remove","htop"));
        for(String action:new String[]{"install","remove","upgrade"})assertFalse(PackageCatalog.operation(action,"bash").contains("-y "));
    }
    @Test public void heldCodexProfileProducesIndependentCorrectEnvironment(){
        WorkspaceStore.Session source=new WorkspaceStore.Session();source.name="Codex";source.environment="codex";source.codexEnvironment="ubuntu";source.directory="/root/my-project";source.startup="codex";source.handle="running-codex";source.saved=true;
        WorkspaceStore.Session operation=PackageCatalog.operationProfile(source,PackageCatalog.operation("install","htop"));
        assertEquals("ubuntu",operation.environment);assertEquals("~",operation.directory);assertEquals("",operation.handle);assertFalse(operation.saved);assertNotEquals(source.id,operation.id);assertEquals("running-codex",source.handle);assertEquals("codex",source.startup);
        source.codexEnvironment="termux";assertEquals("termux",PackageCatalog.operationProfile(source,"apt-get update").environment);
        source.environment="ssh";try{PackageCatalog.operationProfile(source,"apt-get update");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void selectedDestinationRejectsRestartedOrChangedEnvironment(){
        WorkspaceStore.Session profile=new WorkspaceStore.Session();profile.environment="codex";profile.codexEnvironment="ubuntu";
        assertTrue(PackageCatalog.matchesTarget(profile,"held","held","ubuntu"));
        assertFalse(PackageCatalog.matchesTarget(profile,"held","new-terminal","ubuntu"));
        assertFalse(PackageCatalog.matchesTarget(profile,"held","held","termux"));
        assertFalse(PackageCatalog.matchesTarget(profile,"","","ubuntu"));
    }
    @Test public void versionMetadataDeduplicatesAndTracksDowngrade()throws Exception{
        List<PackageCatalog.Entry> versions=PackageCatalog.parseVersions("bash",data("5.2-1\t0\n5.1-1\t1\n5.2-1\t0\n"));
        assertEquals(2,versions.size());assertFalse(versions.get(0).downgrade);assertTrue(versions.get(1).downgrade);
        assertEquals("apt-get install -- 'bash=5.1-1'",PackageCatalog.versionOperation(versions.get(1)));
        assertTrue(PackageCatalog.parseVersions("bash",data("")).isEmpty());assertTrue(PackageCatalog.validVersion("1:2.0~rc1-3+deb12u1"));
    }
    @Test public void unsafeVersionsAndMalformedMetadataRejected()throws Exception{
        for(String value:new String[]{"-y","1;id","1$(id)","1'bad","1\n2",""}){
            assertFalse(PackageCatalog.validVersion(value));try{PackageCatalog.versionOperation(new PackageCatalog.Entry("bash",value));fail();}catch(IllegalArgumentException expected){}
        }
        for(String text:new String[]{"1.0\t2\n","bad\t0\n","1.0\n"}){try{PackageCatalog.parseVersions("bash",data(text));fail();}catch(IOException expected){}}
    }
    @Test public void realVersionQueryUsesDpkgComparisonAndNeverInstalls()throws Exception{
        Process process=new ProcessBuilder("bash","-c",PackageCatalog.versionsQuery("bash")).redirectError(ProcessBuilder.Redirect.INHERIT).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=process.getInputStream().read(b))!=-1)out.write(b,0,n);
        assertTrue(process.waitFor(15,TimeUnit.SECONDS));assertEquals(0,process.exitValue());assertFalse(PackageCatalog.parseVersions("bash",out.toByteArray()).isEmpty());
        assertFalse(PackageCatalog.versionsQuery("bash").contains("apt-get"));
    }
    @Test public void descriptionsHandleParagraphsAndOnlyFirstRecord()throws Exception{
        String metadata="Package: bash\nDescription-en: GNU shell\n A long explanation.\n .\n Second paragraph.\nDescription-md5: ignored\n\nDescription-en: other version\n";
        assertEquals("GNU shell\nA long explanation.\n\nSecond paragraph.",PackageCatalog.parseDescription(data(metadata)));
        assertEquals("Кратко\nПодробно",PackageCatalog.parseDescription(data("Description: Кратко\n Подробно\n")));
        assertEquals("",PackageCatalog.parseDescription(data("Package: missing\n")));
        try{PackageCatalog.parseDescription(new byte[256*1024+1]);fail();}catch(IOException expected){}
    }
    @Test public void descriptionsQueryExactAvailableVersionAndInstalledMetadata()throws Exception{
        PackageCatalog.Entry entry=new PackageCatalog.Entry("bash","5.2-1");
        assertTrue(PackageCatalog.descriptionQuery(entry,false).endsWith("'bash=5.2-1'"));assertTrue(PackageCatalog.descriptionQuery(entry,true).contains("dpkg-query"));
        Process process=new ProcessBuilder("bash","-c",PackageCatalog.descriptionQuery(entry,true)).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=process.getInputStream().read(b))!=-1)out.write(b,0,n);assertTrue(process.waitFor(15,TimeUnit.SECONDS));assertEquals(0,process.exitValue());assertFalse(PackageCatalog.parseDescription(out.toByteArray()).isEmpty());
    }
    @Test public void realDpkgReadAndAllGeneratedScriptsHaveValidSyntax()throws Exception{
        Process p=new ProcessBuilder("bash","-c",PackageCatalog.query(0)).redirectError(ProcessBuilder.Redirect.INHERIT).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)out.write(b,0,n);assertTrue(p.waitFor(15,TimeUnit.SECONDS));assertEquals(0,p.exitValue());assertFalse(PackageCatalog.parse(out.toByteArray(),0).isEmpty());
        for(int tab=0;tab<3;tab++){Process syntax=new ProcessBuilder("bash","-n","-c",PackageCatalog.query(tab)).start();assertEquals(0,syntax.waitFor());}
        for(String action:new String[]{"refresh","install","upgrade","remove"}){Process syntax=new ProcessBuilder("bash","-n","-c",PackageCatalog.operation(action,"libc++:arm64")).start();assertEquals(0,syntax.waitFor());}
    }
}
