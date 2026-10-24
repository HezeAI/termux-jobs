package com.termux.filepicker;

import java.io.*;
import java.nio.file.Files;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=TermuxFileReceiverTest.ExclusiveOpen.class)
public class TermuxFileReceiverTest {
    // Robolectric's default open shadow ignores O_EXCL/O_NOFOLLOW. Model that
    // syscall contract explicitly; directory/file I/O below still uses private real files.
    @org.robolectric.annotation.Implements(android.system.Os.class)
    public static class ExclusiveOpen {
        private static final java.util.List<FileOutputStream> owners=new java.util.ArrayList<>();
        @org.robolectric.annotation.Implementation
        protected static java.io.FileDescriptor open(String path,int flags,int mode)throws android.system.ErrnoException{
            assertEquals(android.system.OsConstants.O_WRONLY | android.system.OsConstants.O_CREAT
                | android.system.OsConstants.O_EXCL | android.system.OsConstants.O_NOFOLLOW, flags);
            assertEquals(0600,mode);
            try{
                Files.createFile(new File(path).toPath());
                FileOutputStream stream=new FileOutputStream(path);owners.add(stream);return stream.getFD();
            }catch(java.nio.file.FileAlreadyExistsException e){throw new android.system.ErrnoException("open",android.system.OsConstants.EEXIST);}
            catch(IOException e){throw new android.system.ErrnoException("open",android.system.OsConstants.EIO);}
        }
    }
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    private Receiver receiver;
    static class Receiver extends TermuxFileReceiverActivity {
        File directory; String error;
        @Override File receiveDirectory(){return directory;}
        @Override void showErrorDialogAndQuit(String message){error=message;}
    }
    @Before public void setup()throws Exception{receiver=new Receiver();receiver.directory=tmp.newFolder("downloads");}
    private File save(String name){return receiver.saveStreamWithName(new ByteArrayInputStream(new byte[]{7,8}),name);}
    @Test public void traversalCannotWriteOutsideDownloads()throws Exception{
        assertNull(save("../escape"));assertFalse(new File(tmp.getRoot(),"escape").exists());
    }
    @Test public void pathLikeNamesAndNullInputAreRejected(){
        for(String name:new String[]{"", ".", "..", "nested/file", "nested\\file", "/tmp/file", "a\u0000b"}) assertNull(name,save(name));
        assertNull(receiver.saveStreamWithName(null,"valid.txt"));
    }
    @Test public void existingFileIsNeverTruncated()throws Exception{
        File existing=new File(receiver.directory,"existing");Files.write(existing.toPath(),new byte[]{42});
        assertNull(save("existing"));assertArrayEquals(new byte[]{42},Files.readAllBytes(existing.toPath()));
    }
    @Test public void symlinkCannotOverwriteItsTarget()throws Exception{
        File target=tmp.newFile("target");Files.write(target.toPath(),new byte[]{42});
        Files.createSymbolicLink(new File(receiver.directory,"link").toPath(),target.toPath());
        assertNull(save("link"));assertArrayEquals(new byte[]{42},Files.readAllBytes(target.toPath()));
    }
    @Test public void normalUnicodeBasenameIsSaved()throws Exception{
        File result=save("Фото 1.txt");assertNotNull(receiver.error,result);
        assertEquals(receiver.directory,result.getParentFile());assertArrayEquals(new byte[]{7,8},Files.readAllBytes(result.toPath()));
    }
    @Test public void inputClosesOnSuccessAndRejectedName(){
        for(String name:new String[]{"new.txt","../bad"}){
            final boolean[] closed={false};InputStream input=new ByteArrayInputStream(new byte[]{1}){
                @Override public void close(){closed[0]=true;}
            };
            receiver.saveStreamWithName(input,name);assertTrue(closed[0]);
        }
    }
    @Test public void failedReadRemovesOnlyNewPartialFile()throws Exception{
        File untouched=tmp.newFile("untouched");
        InputStream broken=new InputStream(){@Override public int read()throws IOException{throw new IOException("synthetic read failure");}};
        assertNull(receiver.saveStreamWithName(broken,"partial"));assertFalse(new File(receiver.directory,"partial").exists());assertTrue(untouched.exists());
    }
}
