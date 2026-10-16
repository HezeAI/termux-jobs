package com.termux.app.workspace;

import com.termux.shared.file.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Real temporary filesystem operations; never touches the installed Termux paths. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=FileUtilsRuntimeTest.RealStat.class)
public class FileUtilsRuntimeTest {
    /** Supply real lstat semantics: Robolectric 4.4 otherwise reports removed files as present. */
    @org.robolectric.annotation.Implements(android.system.Os.class)
    public static class RealStat {
        @org.robolectric.annotation.Implementation protected static android.system.StructStat lstat(String name)throws android.system.ErrnoException{return attributes(name,false);}
        @org.robolectric.annotation.Implementation protected static android.system.StructStat stat(String name)throws android.system.ErrnoException{return attributes(name,true);}
        private static android.system.StructStat attributes(String name,boolean follow)throws android.system.ErrnoException{
            try{
                java.nio.file.attribute.BasicFileAttributes a=Files.readAttributes(Paths.get(name),java.nio.file.attribute.BasicFileAttributes.class,follow?new LinkOption[0]:new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
                int type=a.isDirectory()?android.system.OsConstants.S_IFDIR:a.isSymbolicLink()?android.system.OsConstants.S_IFLNK:android.system.OsConstants.S_IFREG;
                return new android.system.StructStat(0,0,type|0700,1,0,0,0,a.size(),0,0,0,4096,0);
            }catch(java.io.IOException e){throw new android.system.ErrnoException("lstat",android.system.OsConstants.ENOENT,e);}
        }
    }
    @After public void resetApi(){org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",28);}
    @Rule public org.junit.rules.TemporaryFolder temp=new org.junit.rules.TemporaryFolder();
    @Test public void clearDirectoryPreservesRootAndExternalSymlinkTarget()throws Exception{
        for(int api:new int[]{26,28}){org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",api);
        Path root=temp.newFolder("clear"+api).toPath(),outside=temp.newFolder("outside"+api).toPath();
        Files.write(outside.resolve("keep"),new byte[]{7});Files.createDirectories(root.resolve("nested"));Files.write(root.resolve("nested/file"),new byte[]{1});Files.createSymbolicLink(root.resolve("link"),outside);
        assertNull(FileUtils.clearDirectory(root.toString()));assertTrue(Files.isDirectory(root));try(java.util.stream.Stream<Path> entries=Files.list(root)){assertEquals(0,entries.count());}assertTrue(Files.exists(outside.resolve("keep")));
        }
    }
    @Test public void rotateExistingCrashLogReplacesBackupWithoutDeletingOtherFiles()throws Exception{
        for(int api:new int[]{26,28}){org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",api);
        Path root=temp.newFolder("logs"+api).toPath(),source=root.resolve("crash_log.md"),backup=root.resolve("crash_log_backup.md"),keep=root.resolve("keep");
        Files.write(source,"new crash".getBytes(StandardCharsets.UTF_8));Files.write(backup,"old crash".getBytes(StandardCharsets.UTF_8));Files.write(keep,new byte[]{4});
        assertNull(FileUtils.moveRegularFile("test log",source.toString(),backup.toString(),false));assertFalse(Files.exists(source));assertEquals("new crash",new String(Files.readAllBytes(backup),StandardCharsets.UTF_8));assertTrue(Files.exists(keep));
        }
    }
    @Test public void deleteTreeDoesNotFollowSymlinksAndWrongTypeIsRejected()throws Exception{
        for(int api:new int[]{26,28}){org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION.class,"SDK_INT",api);
        Path root=temp.newFolder("delete"+api).toPath(),outside=temp.newFolder("safe"+api).toPath();Files.write(outside.resolve("keep"),new byte[]{9});Files.createSymbolicLink(root.resolve("link"),outside);Files.write(root.resolve("file"),new byte[]{2});
        assertNull(FileUtils.deleteFile("test tree",root.toString(),false));assertFalse(Files.exists(root));assertTrue(Files.exists(outside.resolve("keep")));assertNotNull(FileUtils.clearDirectory(outside.resolve("keep").toString()));
        }
    }
}
