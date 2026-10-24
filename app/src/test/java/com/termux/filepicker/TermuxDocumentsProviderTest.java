package com.termux.filepicker;

import android.database.Cursor;
import android.os.*;
import android.provider.DocumentsContract.Document;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, application=android.app.Application.class, shadows={ProviderTestOs.class, ProviderTestOs.Descriptor.class})
public class TermuxDocumentsProviderTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    File home, outside; Provider provider; Locale original;
    static class Provider extends TermuxDocumentsProvider {
        File home;
        @Override protected File getBaseDirectory() { return home; }
    }
    @Before public void setup() throws Exception {
        ProviderTestOs.reset();
        org.robolectric.shadows.ShadowMimeTypeMap mime = org.robolectric.shadow.api.Shadow.extract(android.webkit.MimeTypeMap.getSingleton());
        mime.addExtensionMimeTypMapping("gif", "image/gif"); home = tmp.newFolder("home"); outside = tmp.newFolder("home-other");
        provider = new Provider(); provider.home = home; original = Locale.getDefault();
    }
    @After public void cleanup() throws Exception { Locale.setDefault(original); try { ProviderTestOs.assertClosed(); } finally { ProviderTestOs.reset(); } }
    interface Checked { void run() throws Exception; }
    void rejected(Checked call) throws Exception { try { call.run(); fail("accepted invalid document"); } catch (FileNotFoundException expected) {} }
    File file(File dir, String name) throws Exception { File f=new File(dir,name); Files.write(f.toPath(),new byte[]{42,43}); return f; }
    int flags(String id) throws Exception { try(Cursor c=provider.queryDocument(id,null)){assertTrue(c.moveToFirst());return c.getInt(c.getColumnIndexOrThrow(Document.COLUMN_FLAGS));} }
    @Test public void traversalOutsideAndPrefixSiblingAreRejectedEverywhere() throws Exception {
        File secret=file(outside,"secret.txt");
        for(String id:new String[]{secret.getPath(), new File(home,"../home-other/secret.txt").getPath(), "relative", "", null}) {
            rejected(()->provider.queryDocument(id,null)); rejected(()->provider.getDocumentType(id));
            rejected(()->provider.openDocument(id,"r",null)); rejected(()->provider.deleteDocument(id));
            rejected(()->provider.createDocument(id,"text/plain","new.txt"));
            assertFalse(provider.isChildDocument(home.getPath(),id));
        }
        assertArrayEquals(new byte[]{42,43},Files.readAllBytes(secret.toPath()));
    }
    @Test public void childRelationshipUsesCanonicalComponentsAndExcludesSelf() throws Exception {
        File dir=new File(home,"dir");dir.mkdir();File child=file(dir,"a.txt"), sibling=file(home,"dir-other");
        assertTrue(provider.isChildDocument(home.getPath(),child.getPath()));
        assertFalse(provider.isChildDocument(dir.getPath(),sibling.getPath()));
        assertFalse(provider.isChildDocument(home.getPath(),home.getPath()));
        assertFalse(provider.isChildDocument(child.getPath(),child.getPath()+"/x"));
    }
    @Test public void escapingLinksAreNotListedOrOpenedAndInternalLinksKeepIds() throws Exception {
        File target=file(home,"actual.txt"), secret=file(outside,"secret.txt");
        File safe=new File(home,"safe-link.txt"), escape=new File(home,"escape.txt");
        Files.createSymbolicLink(safe.toPath(),target.toPath());Files.createSymbolicLink(escape.toPath(),secret.toPath());
        Set<String> ids=new HashSet<>();try(Cursor c=provider.queryChildDocuments(home.getPath(),null,(String)null)){
            while(c.moveToNext())ids.add(c.getString(c.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)));
        }
        assertTrue(ids.contains(safe.getPath()));assertFalse(ids.contains(escape.getPath()));
        rejected(()->provider.openDocument(escape.getPath(),"wt",null));
        try(ParcelFileDescriptor p=provider.openDocument(safe.getPath(),"r",null)){assertArrayEquals(new byte[]{42,43},Files.readAllBytes(Paths.get("/proc/self/fd/"+p.getFd())));}
        provider.deleteDocument(safe.getPath());assertFalse(Files.exists(safe.toPath(),LinkOption.NOFOLLOW_LINKS));assertTrue(target.exists());
    }
    @Test(timeout=10000) public void searchCyclesTerminateAndMixedCaseWorksInTurkish() throws Exception {
        Locale.setDefault(new Locale("tr","TR"));file(home,"IMAGE.GIF");file(outside,"IMAGE-SECRET.GIF");
        File dir=new File(home,"dir");dir.mkdir();Files.createSymbolicLink(new File(dir,"loop").toPath(),home.toPath());
        Files.createSymbolicLink(new File(home,"escape").toPath(),outside.toPath());
        try(Cursor c=provider.querySearchDocuments(home.getPath(),"iMaGe",null)){assertEquals(1,c.getCount());}
        try(Cursor c=provider.querySearchDocuments(home.getPath(),"missing",null)){assertEquals(0,c.getCount());}
        assertEquals("image/gif",provider.getDocumentType(new File(home,"IMAGE.GIF").getPath()));
    }
    @Test public void rootCannotBeDeletedAndHasNoDeleteFlag() throws Exception {
        assertEquals(0,flags(home.getPath())&Document.FLAG_SUPPORTS_DELETE);
        rejected(()->provider.deleteDocument(home.getPath()));assertTrue(home.exists());
    }
    @Test public void childQueryOnFileFailsCleanly() throws Exception {
        File f=file(home,"plain");rejected(()->provider.queryChildDocuments(f.getPath(),null,(String)null));
    }
    @Test public void invalidDisplayNamesCannotCreateAnything() throws Exception {
        for(String name:new String[]{null,"",".","..","../escape","a/b","a\\b","a\0b","/absolute"})
            rejected(()->provider.createDocument(home.getPath(),"text/plain",name));
        assertEquals(0,home.list().length);assertEquals(0,outside.list().length);
    }
    @Test public void creationPreservesExistingAndDanglingLinksAndCreatesDirectories() throws Exception {
        File existing=file(home,"note.txt");
        Files.createSymbolicLink(new File(home,"note.txt (2)").toPath(),new File(outside,"absent").toPath());
        String id=provider.createDocument(home.getPath(),"text/plain","note.txt");
        assertEquals(new File(home,"note.txt (3)").getPath(),id);assertTrue(new File(id).isFile());
        assertArrayEquals(new byte[]{42,43},Files.readAllBytes(existing.toPath()));
        String dir=provider.createDocument(home.getPath(),Document.MIME_TYPE_DIR,"Новая папка");assertTrue(new File(dir).isDirectory());
    }
    @Test public void cancelledOpenAndThumbnailDoNotTruncate() throws Exception {
        File f=file(home,"image.png");CancellationSignal signal=new CancellationSignal();signal.cancel();
        try { provider.openDocument(f.getPath(),"wt",signal);fail(); }catch(OperationCanceledException expected){}
        try { provider.openDocumentThumbnail(f.getPath(),null,signal);fail(); }catch(OperationCanceledException expected){}
        assertEquals(2,f.length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void successfulOpenTruncatesOnlyAfterValidationAndClosesOnLateCancel() throws Exception {
        File f=file(home,"plain.txt");
        try(ParcelFileDescriptor p=provider.openDocument(f.getPath(),"wt",null)){assertEquals(0,f.length());}
        Files.write(f.toPath(),new byte[]{42});CancellationSignal signal=new CancellationSignal();ProviderTestOs.afterOpen=signal::cancel;
        try { provider.openDocument(f.getPath(),"wt",signal);fail(); }catch(OperationCanceledException expected){}
        assertEquals(1,f.length());assertEquals(1,ProviderTestOs.truncations);
    }
    @Test public void finalSymlinkSwapIsRejectedWithoutTruncation() throws Exception {
        File f=file(home,"plain"), target=file(outside,"secret");
        ProviderTestOs.beforeOpen=()->{try{Files.delete(f.toPath());Files.createSymbolicLink(f.toPath(),target.toPath());}catch(IOException e){throw new AssertionError(e);}};
        rejected(()->provider.openDocument(f.getPath(),"wt",null));assertEquals(2,target.length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void movedAncestorIsRejectedByOpenedDescriptorBeforeTruncation() throws Exception {
        File dir=new File(home,"dir");dir.mkdir();File f=file(dir,"plain"), moved=new File(outside,"moved");
        ProviderTestOs.afterOpen=()->{try{Files.move(dir.toPath(),moved.toPath());}catch(IOException e){throw new AssertionError(e);}};
        rejected(()->provider.openDocument(f.getPath(),"wt",null));assertEquals(2,new File(moved,"plain").length());assertEquals(0,ProviderTestOs.truncations);
    }
    @Test public void internalAliasKeepsItsMimeAndRemainsSearchableAfterTargetVisit() throws Exception {
        File target=file(home,"actual.txt"), alias=new File(home,"IMAGE.GIF");
        Files.createSymbolicLink(alias.toPath(),target.toPath());
        assertEquals("image/gif",provider.getDocumentType(alias.getPath()));
        try(Cursor c=provider.querySearchDocuments(home.getPath(),"image",null)){
            assertEquals(1,c.getCount());assertTrue(c.moveToFirst());
            assertEquals(alias.getPath(),c.getString(c.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)));
        }
    }
    @Test public void kernelMountAliasRequiresSameDirectoryInode() throws Exception {
        File f=file(home,"file.txt");
        ProviderTestOs.kernelAlias=new File(tmp.getRoot(),"kernel-alias").toPath();
        ProviderTestOs.aliasTarget=home.toPath();
        try(ParcelFileDescriptor p=provider.openDocument(f.getPath(),"r",null)){assertTrue(p.getFileDescriptor().valid());}
        try(ParcelFileDescriptor p=ProviderFiles.open(f.getPath(),"r",null,
            new File[]{home,new File(tmp.getRoot(),"unmounted-storage")},new File[0])){assertTrue(p.getFileDescriptor().valid());}
    }
    @Test public void aliasRootKeepsLexicalDocumentIds() throws Exception {
        File alias=new File(tmp.getRoot(),"alias");Files.createSymbolicLink(alias.toPath(),home.toPath());provider.home=alias;file(home,"file.txt");
        try(Cursor c=provider.queryChildDocuments(alias.getPath(),null,(String)null)){assertTrue(c.moveToFirst());assertEquals(new File(alias,"file.txt").getPath(),c.getString(c.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)));}
        try(Cursor c=provider.querySearchDocuments(alias.getPath(),"file",null)){assertTrue(c.moveToFirst());assertEquals(new File(alias,"file.txt").getPath(),c.getString(c.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)));}
    }
}
