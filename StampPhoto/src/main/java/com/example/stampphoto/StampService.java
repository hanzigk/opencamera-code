package com.example.stampphoto;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import net.sourceforge.opencamera.ImageSaver;
import net.sourceforge.opencamera.MainActivity;
import net.sourceforge.opencamera.MyApplicationInterface;
import net.sourceforge.opencamera.MyDebug;
import net.sourceforge.opencamera.TextFormatter;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StampService extends Service {
    String TAG = "StampService";
    private final Paint p = new Paint();
    Bitmap bitmap;
    File exifTempFile;
    MainActivity mainActivity = (MainActivity) MainActivity.SecondClass.mActivityRef.get();
    ImageSaver.Request request;
    MyApplicationInterface applicationInterface = mainActivity.getApplicationInterface();
    ImageSaver imageSaver = new ImageSaver(mainActivity);
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        bitmap = ImageSaver.stamp_bitmap;
        request = ImageSaver.stamp_request;
        boolean dategeo_stamp = request.preference_stamp.equals("preference_stamp_yes");

        boolean text_stamp = request.preference_textstamp.length() > 0;
        exifTempFile = intent.getParcelableExtra("exifTempFile");
        byte [] data = intent.getByteArrayExtra("data");
        if (bitmap == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "decode bitmap in order to stamp info");
            bitmap = imageSaver.loadBitmapWithRotation(data, true, exifTempFile);
            if (bitmap == null) {
                mainActivity.getPreview().showToast(null, net.sourceforge.opencamera.R.string.failed_to_stamp);
                System.gc();
            }
        }
        if (bitmap != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "stamp info to bitmap: " + bitmap);
            if (MyDebug.LOG)
                Log.d(TAG, "bitmap is mutable?: " + bitmap.isMutable());
            int font_size = request.font_size;
            int color = request.color;
            String pref_style = request.pref_style;
            if (MyDebug.LOG)
                Log.d(TAG, "pref_style: " + pref_style);
            String preference_stamp_dateformat = request.preference_stamp_dateformat;
            String preference_stamp_timeformat = request.preference_stamp_timeformat;
            String preference_stamp_gpsformat = request.preference_stamp_gpsformat;


            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (MyDebug.LOG) {
                Log.d(TAG, "decoded bitmap size " + width + ", " + height);
                Log.d(TAG, "bitmap size: " + width * height * 4);
            }
            Canvas canvas = new Canvas(bitmap);
            p.setColor(Color.WHITE);
            // we don't use the density of the screen, because we're stamping to the image, not drawing on the screen (we don't want the font height to depend on the device's resolution)
            // instead we go by 1 pt == 1/72 inch height, and scale for an image height (or width if in portrait) of 4" (this means the font height is also independent of the photo resolution)
            int smallest_size = (width < height) ? width : height;
            float scale = ((float) smallest_size) / (72.0f * 4.0f);
            int font_size_pixel = (int) (font_size * scale + 0.5f); // convert pt to pixels
            if (MyDebug.LOG) {
                Log.d(TAG, "scale: " + scale);
                Log.d(TAG, "font_size: " + font_size);
                Log.d(TAG, "font_size_pixel: " + font_size_pixel);
            }
            p.setTextSize(font_size_pixel);
            int offset_x = (int) (8 * scale + 0.5f); // convert pt to pixels
            int offset_y = (int) (8 * scale + 0.5f); // convert pt to pixels
            int diff_y = (int) ((font_size + 4) * scale + 0.5f); // convert pt to pixels
            int ypos = height - offset_y;
            p.setTextAlign(Paint.Align.RIGHT);
            MyApplicationInterface.Shadow draw_shadowed = MyApplicationInterface.Shadow.SHADOW_NONE;
            switch (pref_style) {
                case "preference_stamp_style_shadowed":
                    draw_shadowed = MyApplicationInterface.Shadow.SHADOW_OUTLINE;
                    break;
                case "preference_stamp_style_plain":
                    draw_shadowed = MyApplicationInterface.Shadow.SHADOW_NONE;
                    break;
                case "preference_stamp_style_background":
                    draw_shadowed = MyApplicationInterface.Shadow.SHADOW_BACKGROUND;
                    break;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "draw_shadowed: " + draw_shadowed);
            if (dategeo_stamp) {
                if (MyDebug.LOG)
                    Log.d(TAG, "stamp date");
                // doesn't respect user preferences such as 12/24 hour - see note about in draw() about DateFormat.getTimeInstance()
                String date_stamp = TextFormatter.getDateString(preference_stamp_dateformat, request.current_date);
                String time_stamp = TextFormatter.getTimeString(preference_stamp_timeformat, request.current_date);
                if (MyDebug.LOG) {
                    Log.d(TAG, "date_stamp: " + date_stamp);
                    Log.d(TAG, "time_stamp: " + time_stamp);
                }
                if (date_stamp.length() > 0 || time_stamp.length() > 0) {
                    String datetime_stamp = "";
                    if (date_stamp.length() > 0)
                        datetime_stamp += date_stamp;
                    if (time_stamp.length() > 0) {
                        if (datetime_stamp.length() > 0)
                            datetime_stamp += " ";
                        datetime_stamp += time_stamp;
                    }
                    applicationInterface.drawTextWithBackground(canvas, p, datetime_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                }
                ypos -= diff_y;
                String gps_stamp = mainActivity.getTextFormatter().getGPSString(preference_stamp_gpsformat, request.preference_units_distance, request.store_location, request.location, request.store_geo_direction, request.geo_direction);
                if (gps_stamp.length() > 0) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "stamp with location_string: " + gps_stamp);

                    Address address = null;
                    if (request.store_location && !request.preference_stamp_geo_address.equals("preference_stamp_geo_address_no")) {
                        // try to find an address
                        // n.b., if we update the class being used, consider whether the info on Geocoder in preference_stamp_geo_address_summary needs updating
                        if (Geocoder.isPresent()) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "geocoder is present");
                            Geocoder geocoder = new Geocoder(mainActivity, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(request.location.getLatitude(), request.location.getLongitude(), 1);
                                if (addresses != null && addresses.size() > 0) {
                                    address = addresses.get(0);
                                    if (MyDebug.LOG) {
                                        Log.d(TAG, "address: " + address);
                                        Log.d(TAG, "max line index: " + address.getMaxAddressLineIndex());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "failed to read from geocoder");
                                e.printStackTrace();
                            }
                        } else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "geocoder not present");
                        }
                    }

                    if (address == null || request.preference_stamp_geo_address.equals("preference_stamp_geo_address_both")) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "display gps coords");
                        // want GPS coords (either in addition to the address, or we don't have an address)
                        // we'll also enter here if store_location is false, but we have geo direction to display
                        applicationInterface.drawTextWithBackground(canvas, p, gps_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                        ypos -= diff_y;
                    } else if (request.store_geo_direction) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "not displaying gps coords, but need to display geo direction");
                        // we are displaying an address instead of GPS coords, but we still need to display the geo direction
                        gps_stamp = mainActivity.getTextFormatter().getGPSString(preference_stamp_gpsformat, request.preference_units_distance, false, null, request.store_geo_direction, request.geo_direction);
                        if (gps_stamp.length() > 0) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "gps_stamp is now: " + gps_stamp);
                            applicationInterface.drawTextWithBackground(canvas, p, gps_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                            ypos -= diff_y;
                        }
                    }

                    if (address != null) {
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            // write in reverse order
                            String addressLine = address.getAddressLine(address.getMaxAddressLineIndex() - i);
                            applicationInterface.drawTextWithBackground(canvas, p, addressLine, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                            ypos -= diff_y;
                        }
                    }
                }
            }
            if (text_stamp) {
                if (MyDebug.LOG)
                    Log.d(TAG, "stamp text");
                applicationInterface.drawTextWithBackground(canvas, p, request.preference_textstamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                ypos -= diff_y;
            }
        }
        ImageSaver.stamp_bitmap = bitmap;
        ImageSaver.stamp_exifTempFile = exifTempFile;
        ImageSaver.stamp_flag = true;
        stopSelf();
        return Service.START_NOT_STICKY;
    }

}

