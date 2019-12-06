package com.example.gallery;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import net.sourceforge.opencamerahzz.MyDebug;
import net.sourceforge.opencamerahzz.preview.BasicApplicationInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GalleryInterface extends BasicApplicationInterface {
    private static final String TAG = "GalleryInterface";
    private final StorageUtils storageUtils;
    private final GalleryActivity galleryActivity;
    private static class LastImage {
        final boolean share; // one of the images in the list should have share set to true, to indicate which image to share
        final String name;
        Uri uri;

        LastImage(Uri uri, boolean share) {
            this.name = null;
            this.uri = uri;
            this.share = share;
        }

        LastImage(String filename, boolean share) {
            this.name = filename;
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
                // previous to Android 7, we could just use a "file://" uri, but this is no longer supported on Android 7, and
                // results in a android.os.FileUriExposedException when trying to share!
                // see https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
                // so instead we leave null for now, and set it from MyApplicationInterface.scannedFile().
                this.uri = null;
            }
            else {
                this.uri = Uri.parse("file://" + this.name);
            }
            this.share = share;
        }
    }
    private final List<LastImage> last_images = new ArrayList<>();
    public GalleryInterface(GalleryActivity galleryActivity, Bundle savedInstanceState)
    {
        this.galleryActivity = galleryActivity;
        this.storageUtils = new StorageUtils(galleryActivity,this);
    }
    void scannedFile(File file, Uri uri) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "scannedFile");
            Log.d(TAG, "file: " + file);
            Log.d(TAG, "uri: " + uri);
        }
        // see note under LastImage constructor for why we need to update the Uris
        for(int i=0;i<last_images.size();i++) {
            LastImage last_image = last_images.get(i);
            if( MyDebug.LOG )
                Log.d(TAG, "compare to last_image: " + last_image.name);
            if( last_image.uri == null && last_image.name != null && last_image.name.equals(file.getAbsolutePath()) ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "updated last_image : " + i);
                last_image.uri = uri;
            }
        }
    }
    public StorageUtils getStorageUtils() {
        return storageUtils;
    }
    @Override
    public Context getContext() {
        return galleryActivity;
    }

    @Override
    public boolean useCamera2() {
        return false;
    }

    @Override
    public int createOutputVideoMethod() {
        return 0;
    }

    @Override
    public File createOutputVideoFile(String extension) throws IOException {
        return null;
    }

    @Override
    public Uri createOutputVideoSAF(String extension) throws IOException {
        return null;
    }

    @Override
    public Uri createOutputVideoUri() {
        return null;
    }

    @Override
    public void requestCameraPermission() {

    }

    @Override
    public boolean needsStoragePermission() {
        return false;
    }

    @Override
    public void requestStoragePermission() {

    }

    @Override
    public void requestRecordAudioPermission() {

    }

    @Override
    public boolean onPictureTaken(byte[] data, Date current_date) {
        return false;
    }
}
