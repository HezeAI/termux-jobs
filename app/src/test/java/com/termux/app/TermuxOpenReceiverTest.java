package com.termux.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import com.termux.filepicker.ProviderTestOs;
import com.termux.shared.termux.TermuxConstants;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=android.app.Application.class,shadows={ProviderTestOs.class,ProviderTestOs.Descriptor.class})
public class TermuxOpenReceiverTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    File root, storage, properties; Provider provider; Locale original;
    static class Provider extends TermuxOpenReceiver.ContentProvider {
        File[] roots, readOnly; String error;
        @Override File[] allowedRoots(){return roots;}
        @Override File[] readOnlyFiles(){return readOnly;}
        @Override String externalAppsPolicyError(){return error;}
    }
    @Before public void setup()throws Exception{
        ProviderTestOs.reset();
        org.robolectric.shadows.ShadowMimeTypeMap mime = org.robolectric.shadow.api.Shadow.extract(android.webkit.MimeTypeMap.getSingleton());
        mime.addExtensionMimeTypMapping("gif", "image/gif");root=tmp.newFolder("files");storage=tmp.newFolder("storage");properties=new File(root,"termux.properties");Files.write(properties.toPath(),new byte[]{42,43});
        provider=new Provider();provider.roots=new File[]{root,storage};provider.readOnly=new File[]{properties};original=Locale.getDefault();
    }
    @After public void cleanup()throws Exception{Locale.setDefault(original);try{ProviderTestOs.assertClosed();}finally{ProviderTestOs.reset();}}
    Uri uri(File file){return new Uri.Builder().scheme("content").authority(TermuxConstants.TERMUX_FILE_SHARE_URI_AUTHORITY).path(file.getPath()).build();}
    interface Checked{void run()throws Exception;}
    void rejected(Checked call)throws Exception{try{call.run();fail("accepted invalid provider URI");}catch(IllegalArgumentException|FileNotFoundException expected){}}
    @Test public void prefixSiblingsAndEscapingLinksAreRejectedForMetadataAndOpen()throws Exception{
        File sibling=tmp.newFolder("files-other"), outside=new File(sibling,"secret.txt");Files.write(outside.toPath(),new byte[]{42});
        File link=new File(root,"link");Files.createSymbolicLink(link.toPath(),outside.toPath());
        for(File f:new File[]{outside,link}){
            Uri uri=uri(f);rejected(()->provider.query(uri,null,null,null,null));rejected(()->provider.getType(uri));rejected(()->provider.openFile(uri,"wt"));
        }
        assertEquals(1,outside.length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void malformedMissingAndDirectoryUrisAreRejected()throws Exception{
        for(Uri uri:new Uri[]{uri(root),uri(new File(root,"missing")),Uri.parse("content://"+TermuxConstants.TERMUX_FILE_SHARE_URI_AUTHORITY),
            uri(properties).buildUpon().fragment("bad").build(),uri(properties).buildUpon().query("bad").build(),uri(properties).buildUpon().authority("other").build()}){
            rejected(()->provider.query(uri,null,null,null,null));rejected(()->provider.getType(uri));rejected(()->provider.openFile(uri,"r"));
        }
    }
    @Test public void metadataRetainsLongSizeBeyondTwoGigabytes()throws Exception{
        File large=new File(root,"large.bin");long size=3L*1024*1024*1024+17;
        try(RandomAccessFile raf=new RandomAccessFile(large,"rw")){raf.setLength(size);}
        try(Cursor c=provider.query(uri(large),null,null,null,null)){assertTrue(c.moveToFirst());assertEquals(size,c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)));}
    }
    @Test public void mimeUsesRootLocaleAndNoExtensionIsUnknown()throws Exception{
        Locale.setDefault(new Locale("tr","TR"));File image=new File(root,"IMAGE.GIF");Files.write(image.toPath(),new byte[]{1});
        assertEquals("image/gif",provider.getType(uri(image)));assertNull(provider.getType(uri(properties)));
        File noExtension=new File(root,"gif");Files.write(noExtension.toPath(),new byte[]{1});assertNull(provider.getType(uri(noExtension)));
    }
    @Test public void allowExternalAppsPolicyStillBlocksOpenBeforeWriting()throws Exception{
        provider.error="synthetic denied policy";rejected(()->provider.openFile(uri(properties),"wt"));assertEquals(2,properties.length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void propertiesAndTheirHardLinkAliasesAreReadOnly()throws Exception{
        File hardlink=new File(root,"alias.txt");Files.createLink(hardlink.toPath(),properties.toPath());
        for(File f:new File[]{properties,hardlink}){
            try(ParcelFileDescriptor p=provider.openFile(uri(f),"wt")){assertArrayEquals(new byte[]{42,43},Files.readAllBytes(Paths.get("/proc/self/fd/"+p.getFd())));}
        }
        assertEquals(2,properties.length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void filesAndSharedStorageBothRemainAvailable()throws Exception{
        File shared=new File(storage,"photo.jpg");Files.write(shared.toPath(),new byte[]{7});
        try(ParcelFileDescriptor p=provider.openFile(uri(shared),"r")){assertEquals(7,Files.readAllBytes(Paths.get("/proc/self/fd/"+p.getFd()))[0]);}
        File normal=new File(root,"normal");Files.write(normal.toPath(),new byte[]{8});
        try(ParcelFileDescriptor p=provider.openFile(uri(normal),"wt")){assertEquals(0,normal.length());}
    }
    @Test public void internalAliasRetainsDisplayNameAndExtension()throws Exception{
        File alias=new File(root,"IMAGE.GIF");Files.createSymbolicLink(alias.toPath(),properties.toPath());
        assertEquals("image/gif",provider.getType(uri(alias)));
        try(Cursor c=provider.query(uri(alias),null,null,null,null)){
            assertTrue(c.moveToFirst());assertEquals("IMAGE.GIF",c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)));
        }
    }
    @Test public void receiverEncodesSpecialNamesAndFallsBackToView()throws Exception{
        Locale.setDefault(new Locale("tr","TR"));File image=new File(root,"photo #1? 100%.GIF");Files.write(image.toPath(),new byte[]{1});
        Context context=RuntimeEnvironment.application;
        new TermuxOpenReceiver().onReceive(context,new Intent("invalid-action",Uri.fromFile(image)));
        Intent sent=Shadows.shadowOf(RuntimeEnvironment.application).getNextStartedActivity();assertNotNull(sent);
        assertEquals(Intent.ACTION_VIEW,sent.getAction());assertEquals("image/gif",sent.getType());assertEquals(image.getAbsolutePath(),sent.getData().getPath());
        assertNull(sent.getData().getQuery());assertNull(sent.getData().getFragment());
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,sent.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
    }
}
