package com.termux.app.workspace;

import android.app.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class EnvironmentPackageVersionsTest {
    private byte[] data(String value){return value.getBytes(StandardCharsets.UTF_8);}
    @Test public void dnfRetainsEpochArchitectureAndRepositoryRatherThanPickingLatest()throws Exception{
        List<EnvironmentPackageVersions.Version> rows=EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.DNF,"bash",data("bash\t5.3-1.fc44\taarch64\tfedora\nbash\t1:5.3-2.fc44\taarch64\tupdates\nbash\t1:5.3-2.fc44\taarch64\tupdates\nbash\t5.3-1.fc44\tsrc\tfedora-source\nother\t1-1\taarch64\tfedora\n"));
        assertEquals(2,rows.size());assertEquals("1:5.3-2.fc44",rows.get(1).version);
        assertEquals("dnf install --from-repo='updates' -- 'bash-1:5.3-2.fc44.aarch64'",EnvironmentPackageVersions.operation(EnvironmentPackages.Kind.DNF,rows.get(1)));
        assertTrue(rows.get(1).label().contains("updates"));
    }
    @Test public void zypperRetainsAllExactMatchesAndExcludesInstalledOnlyAndSources()throws Exception{
        String xml="<stream><search-result><solvable-list>"+
            "<solvable name='7zip' edition='24.09-160000.2.2' arch='aarch64' repository='openSUSE:repo-oss'/>"+
            "<solvable name='7zip' edition='26.02-160000.1.1' arch='aarch64' repository='openSUSE:repo-oss'/>"+
            "<solvable name='7zip' edition='0-1' arch='aarch64' repository='(System Packages)'/>"+
            "<solvable name='7zip' edition='1-1' arch='src' repository='sources'/>"+
            "<solvable name='7zip-extra' edition='1-1' arch='aarch64' repository='extra'/>"+
            "</solvable-list></search-result></stream>";
        List<EnvironmentPackageVersions.Version> rows=EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.ZYPPER,"7zip",data(xml));assertEquals(2,rows.size());
        assertEquals("zypper install --oldpackage --from 'openSUSE:repo-oss' -- '7zip.aarch64=24.09-160000.2.2'",EnvironmentPackageVersions.operation(EnvironmentPackages.Kind.ZYPPER,rows.get(0)));
        List<EnvironmentPackageVersions.Version> named=EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.ZYPPER,"tree",data("<stream><solvable name='tree' edition='2.2.1-1' arch='aarch64' repository='Main Repository (OSS)'/></stream>"));
        assertTrue(EnvironmentPackageVersions.operation(EnvironmentPackages.Kind.ZYPPER,named.get(0)).contains("--from 'Main Repository (OSS)'"));
    }
    @Test public void realAlpinePolicyShapeExcludesInstalledOnlyAndUnrelatedPackages()throws Exception{
        String policy="dotnet-host policy:\n  8.0.32-r0:\n    https://dl-cdn.alpinelinux.org/alpine/v3.24/community\n  10.0.13-r0:\n    https://dl-cdn.alpinelinux.org/alpine/v3.24/community\ntree policy:\n  2.3.2-r0:\n    lib/apk/db/installed\n    https://dl-cdn.alpinelinux.org/alpine/v3.24/main\n  2.2.1-r0:\n    /usr/lib/apk/db/installed\n";
        List<EnvironmentPackageVersions.Version> rows=EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.APK,"tree",data(policy));assertEquals(1,rows.size());assertEquals("2.3.2-r0",rows.get(0).version);
        assertEquals("apk --interactive add -- 'tree=2.3.2-r0'",EnvironmentPackageVersions.operation(EnvironmentPackages.Kind.APK,rows.get(0)));
        assertEquals(2,EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.APK,"dotnet-host",data(policy)).size());
        assertTrue(EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.APK,"missing",data(policy)).isEmpty());
    }
    @Test public void repositoryCredentialsAndQueryAreNotShown()throws Exception{
        List<EnvironmentPackageVersions.Version> rows=EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.APK,"tree",data("tree policy:\n  2.3.2-r0:\n    https://user:password@example.invalid:443/alpine/main?token=private\n"));
        assertEquals("https://example.invalid:443/alpine/main",rows.get(0).repository);assertFalse(rows.get(0).label().contains("password"));assertFalse(rows.get(0).label().contains("private"));
    }
    @Test public void malformedMetadataAndXmlEntitiesFailClosed()throws Exception{
        for(String raw:new String[]{"tree\t1-r0\taarch64\n","tree\t1;id\taarch64\tfedora\n","tree\t1-r0\taarch64\t--bad\n","tree\t1-r0\taarch64\trepo*\n"}){
            try{EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.DNF,"tree",data(raw));fail(raw);}catch(java.io.IOException expected){}
        }
        for(String raw:new String[]{"<!DOCTYPE stream [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><stream>&x;</stream>","<stream><solvable name='tree' edition='1-1' arch='aarch64'/></stream>"}){
            try{EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.ZYPPER,"tree",data(raw));fail();}catch(java.io.IOException expected){}
        }
        try{EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.APK,"tree",data("tree policy:\n  1-r0:\n    file:///tmp/pkg\n"));fail();}catch(java.io.IOException expected){}
        try{EnvironmentPackageVersions.parse(EnvironmentPackages.Kind.DNF,"tree",new byte[EnvironmentPackageVersions.MAX_BYTES+1]);fail();}catch(java.io.IOException expected){}
    }
    @Test public void pickerAndCommandsAreUnavailableForVoidAndArch(){
        for(EnvironmentPackages.Kind kind:new EnvironmentPackages.Kind[]{EnvironmentPackages.Kind.XBPS,EnvironmentPackages.Kind.PACMAN}){
            assertFalse(EnvironmentPackageVersions.supported(kind));
            try{EnvironmentPackageVersions.query(kind,"tree");fail();}catch(IllegalArgumentException expected){}
            try{EnvironmentPackageVersions.operation(kind,new EnvironmentPackageVersions.Version("tree","1","aarch64","main"));fail();}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void generatedQueriesAreReadOnlyAndRetainAllVersions()throws Exception{
        for(EnvironmentPackages.Kind kind:new EnvironmentPackages.Kind[]{EnvironmentPackages.Kind.APK,EnvironmentPackages.Kind.DNF,EnvironmentPackages.Kind.ZYPPER}){
            String command=EnvironmentPackageVersions.query(kind,"tree");
            assertFalse(command.contains("latest-limit"));assertFalse(command.contains("makecache"));assertFalse(command.contains(" update"));assertFalse(command.contains(" refresh"));
            assertTrue(command.contains("--no-network")||command.contains("--cacheonly")||command.contains("--no-refresh"));
            assertEquals(0,new ProcessBuilder("/bin/sh","-n","-c",command).start().waitFor());
            for(String name:new String[]{"-y","$(id)","tree;id","a/b","a\nb"})try{EnvironmentPackageVersions.query(kind,name);fail();}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void explicitOperationsKeepConfirmationAndValidateExactTargets()throws Exception{
        for(EnvironmentPackages.Kind kind:new EnvironmentPackages.Kind[]{EnvironmentPackages.Kind.APK,EnvironmentPackages.Kind.DNF,EnvironmentPackages.Kind.ZYPPER}){
            EnvironmentPackageVersions.Version valid=new EnvironmentPackageVersions.Version("tree","1:2.3-1","aarch64","updates");
            if(kind==EnvironmentPackages.Kind.APK)valid=new EnvironmentPackageVersions.Version("tree","2.3.2-r0","","https://example.invalid/main");
            String command=EnvironmentPackageVersions.operation(kind,valid);assertFalse(command.contains("--force"));assertFalse(command.contains("--no-confirm"));assertFalse(command.contains("--no-gpg"));assertFalse(command.contains("--allow-untrusted"));assertFalse(command.contains(" -y"));
            assertEquals(0,new ProcessBuilder("/bin/sh","-n","-c",command).start().waitFor());
            try{EnvironmentPackageVersions.operation(kind,new EnvironmentPackageVersions.Version("tree","1;touch /tmp/no","aarch64","updates"));fail();}catch(IllegalArgumentException expected){}
        }
    }
}
