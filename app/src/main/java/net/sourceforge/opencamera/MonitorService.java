package net.sourceforge.opencamera;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import net.sourceforge.opencamera.cameracontroller.CameraControllerException;

public class MonitorService extends Service {
    String TAG = "MonitorService";
    MainActivity mainActivity = (MainActivity) MainActivity.SecondClass.mActivityRef.get();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"MonitorService");
        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        this.registerReceiver(this.mMemInfoReciever,
                new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.mBatInfoReceiver);
        this.unregisterReceiver(this.mMemInfoReciever);
    }
    private BroadcastReceiver mMemInfoReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mainActivity.getApplicationInterface().getImageQualityPref()>80)
            {
                AlertDialog.Builder bb = new AlertDialog.Builder(mainActivity);
                bb.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(PreferenceKeys.QualityPreferenceKey, "50");
                        editor.apply();
                        try {
                            mainActivity.preview.setupCameraParameters();
                        } catch (CameraControllerException e) {
                            e.printStackTrace();
                        }
                    }
                });
                bb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                bb.setMessage("Memeory is low, do you want to lower image quality to 50%?");
                bb.setTitle("Warning");
                bb.show();
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {

            int level = intent.getIntExtra("level", 0);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            Boolean result = sharedPreferences.getBoolean(PreferenceKeys.LocationPreferenceKey,false);
            if( level < 20 && result == true)
            {
                AlertDialog.Builder bb = new AlertDialog.Builder(mainActivity);
                bb.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(PreferenceKeys.LocationPreferenceKey, false);
                        editor.apply();
                        mainActivity.getMainUI().updateStoreLocationIcon();
                        mainActivity.getApplicationInterface().getDrawPreview().updateSettings();
                    }
                });
                bb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                bb.setMessage("Battery is low, do you want to disable gps?");
                bb.setTitle("Warning");
                bb.show();
            }
            Log.d(TAG, String.valueOf(level) + "%");

        }
    };
}
