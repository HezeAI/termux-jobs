package com.termux.app.workspace;

import android.app.Application;import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;import java.nio.file.Files;import java.util.Base64;import org.json.JSONObject;import org.junit.*;import org.junit.rules.TemporaryFolder;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowContentResolver;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class ChatGalleryStagingTest {
 @Rule public TemporaryFolder tmp=new TemporaryFolder();
 static final class Provider extends ContentProvider {
  File file;String name="photo.heic",mime="image/heic";long size;int opens;
  @Override public boolean onCreate(){return true;}@Override public String getType(Uri uri){return mime;}
  @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String order){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{name,size});return c;}
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{opens++;return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);}
  @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}@Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}@Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 }
 @Test public void selectedOriginalIsCopiedAndGalleryPhotoGetsModelJpeg()throws Exception{
  Provider p=new Provider();p.file=tmp.newFile("original");byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jWZkAAAAASUVORK5CYII=");Files.write(p.file.toPath(),png);p.size=png.length;ShadowContentResolver.registerProviderInternal("fixture.gallery",p);java.io.ByteArrayInputStream selectedBytes=new java.io.ByteArrayInputStream(png);Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver()).registerInputStream(Uri.parse("content://fixture.gallery/selected"),selectedBytes);WorkspaceStore.Session target=new WorkspaceStore.Session();target.environment="codex";JSONObject a=ChatAttachments.stageUri(RuntimeEnvironment.application,Uri.parse("content://fixture.gallery/selected"),target);
  try{assertEquals("image",a.getString("type"));assertEquals("photo.heic",a.getString("name"));assertTrue(a.has("modelPath"));File original=ChatAttachments.inside(ChatAttachments.root(RuntimeEnvironment.application,target),a.getString("id")+"/original.heic");assertArrayEquals(png,Files.readAllBytes(original.toPath()));assertArrayEquals(png,Files.readAllBytes(p.file.toPath()));assertEquals(0,selectedBytes.available());}finally{ChatAttachments.discard(RuntimeEnvironment.application,a);}
 }
 @Test public void invalidSchemeAndOversizeMetadataNeverReadOriginal()throws Exception{
  Provider p=new Provider();p.file=tmp.newFile("original");p.size=ChatAttachments.MAX_IMAGE+1;ShadowContentResolver.registerProviderInternal("fixture.gallery.limit",p);WorkspaceStore.Session target=new WorkspaceStore.Session();target.environment="codex";
  try{ChatAttachments.stageUri(RuntimeEnvironment.application,Uri.parse("file:///private/key"),target);fail();}catch(IOException expected){}
  try{ChatAttachments.stageUri(RuntimeEnvironment.application,Uri.parse("content://fixture.gallery.limit/selected"),target);fail();}catch(IOException expected){assertEquals("FILE_LIMIT",expected.getMessage());}assertEquals(0,p.opens);
 }
}
