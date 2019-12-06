package com.example.gallery;



import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;



import net.sourceforge.opencamerahzz.MyDebug;
import net.sourceforge.opencamerahzz.preview.Preview;

import java.io.IOException;
import java.util.Locale;

public class GalleryActivity extends Activity {
    public Preview preview;
    private static final String TAG = "GalleryActivity";
    public boolean is_test;
    private GalleryInterface galleryInterface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"enter dynamic module");
        galleryInterface = new GalleryInterface(this,savedInstanceState);
        openGallery();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.finish();
    }

    private void openGallery() {
        if( MyDebug.LOG )
            Log.d(TAG, "openGallery");
        //Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Uri uri = galleryInterface.getStorageUtils().getLastMediaScanned();
        boolean is_raw = false; // note that getLastMediaScanned() will never return RAW images, as we only record JPEGs
        if( uri == null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "go to latest media");
            StorageUtils.Media media = galleryInterface.getStorageUtils().getLatestMedia();
            if( media != null ) {
                uri = media.uri;
                is_raw = media.path != null && media.path.toLowerCase(Locale.US).endsWith(".dng");
            }
        }

        if( uri != null ) {
            // check uri exists
            if( MyDebug.LOG ) {
                Log.d(TAG, "found most recent uri: " + uri);
                Log.d(TAG, "is_raw: " + is_raw);
            }
            try {
                ContentResolver cr = getContentResolver();
                ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
                if( pfd == null ) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "uri no longer exists (1): " + uri);
                    uri = null;
                    is_raw = false;
                }
                else {
                    pfd.close();
                }
            }
            catch(IOException e) {
                if( MyDebug.LOG )
                    Log.d(TAG, "uri no longer exists (2): " + uri);
                uri = null;
                is_raw = false;
            }
        }
        if( uri == null ) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            is_raw = false;
        }
        if( !is_test ) {
            // don't do if testing, as unclear how to exit activity to finish test (for testGallery())
            if( MyDebug.LOG )
                Log.d(TAG, "launch uri:" + uri);
            final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
            boolean done = false;
            if( !is_raw ) {
                // REVIEW_ACTION means we can view video files without autoplaying
                // however, Google Photos at least has problems with going to a RAW photo (in RAW only mode),
                // unless we first pause and resume Open Camera
                if( MyDebug.LOG )
                    Log.d(TAG, "try REVIEW_ACTION");
                try {
                    Intent intent = new Intent(REVIEW_ACTION, uri);
                    this.startActivity(intent);
                    done = true;
                }
                catch(ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if( !done ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "try ACTION_VIEW");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                // see http://stackoverflow.com/questions/11073832/no-activity-found-to-handle-intent - needed to fix crash if no gallery app installed
                //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("blah")); // test
                if( intent.resolveActivity(getPackageManager()) != null ) {
                    try {
                        this.startActivity(intent);
                    }
                    catch(SecurityException e2) {
                        // have received this crash from Google Play - don't display a toast, simply do nothing
                        Log.e(TAG, "SecurityException from ACTION_VIEW startActivity");
                        e2.printStackTrace();
                    }
                }
                else{
                    preview.showToast(null, net.sourceforge.opencamerahzz.R.string.no_gallery_app);
                }
            }
        }
    }
}
