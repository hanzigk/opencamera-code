package com.example.audiocontrol;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;
import android.util.Log;
import net.sourceforge.opencamerahzz.R;
import net.sourceforge.opencamerahzz.MainActivity;
import net.sourceforge.opencamerahzz.MyDebug;
import net.sourceforge.opencamerahzz.PreferenceKeys;
import net.sourceforge.opencamerahzz.ToastBoxer;

import static net.sourceforge.opencamerahzz.MainActivity.SecondClass;

public class AudioControlService extends Service {
    String TAG = "AudioControlService";
    private SpeechControl speechControl;
    MainActivity mainActivity = (MainActivity) SecondClass.mActivityRef.get();
    PermissionHandler permissionHandler;
    private final ToastBoxer audio_control_toast = new ToastBoxer();
    private AudioListener audio_listener;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void freeAudioListener(boolean wait_until_done) {
        if( MyDebug.LOG )
            Log.d(TAG, "freeAudioListener");
        if( audio_listener != null ) {
            audio_listener.release(wait_until_done);
            audio_listener = null;
        }
        mainActivity.mainUI.audioControlStopped();
    }

    private void startAudioListener() {
        if( MyDebug.LOG )
            Log.d(TAG, "startAudioListener");
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            // we restrict the checks to Android 6 or later just in case, see note in LocationSupplier.setupLocationListener()
            if( MyDebug.LOG )
                Log.d(TAG, "check for record audio permission");
            if( ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "record audio permission not available");
                permissionHandler.requestRecordAudioPermission();
                return;
            }
        }

        MyAudioTriggerListenerCallback callback = new MyAudioTriggerListenerCallback(this.mainActivity);
        audio_listener = new AudioListener(callback);
        if( audio_listener.status() ) {
            mainActivity.preview.showToast(audio_control_toast, R.string.audio_listener_started);

            audio_listener.start();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String sensitivity_pref = sharedPreferences.getString(PreferenceKeys.AudioNoiseControlSensitivityPreferenceKey, "0");
            int audio_noise_sensitivity;
            switch(sensitivity_pref) {
                case "3":
                    audio_noise_sensitivity = 50;
                    break;
                case "2":
                    audio_noise_sensitivity = 75;
                    break;
                case "1":
                    audio_noise_sensitivity = 125;
                    break;
                case "-1":
                    audio_noise_sensitivity = 150;
                    break;
                case "-2":
                    audio_noise_sensitivity = 200;
                    break;
                case "-3":
                    audio_noise_sensitivity = 400;
                    break;
                default:
                    // default
                    audio_noise_sensitivity = 100;
                    break;
            }
            callback.setAudioNoiseSensitivity(audio_noise_sensitivity);
            mainActivity.mainUI.audioControlStarted();
        }
        else {
            audio_listener.release(true); // shouldn't be needed, but just to be safe
            audio_listener = null;
            mainActivity.preview.showToast(null, R.string.audio_listener_failed);
        }
    }

	/*void startAudioListeners() {
		initAudioListener();
		// no need to restart speech recognizer, as we didn't free it in stopAudioListeners(), and it's controlled by a user button
	}*/

    public void stopAudioListeners() {
        freeAudioListener(true);
        if( speechControl.hasSpeechRecognition() ) {
            // no need to free the speech recognizer, just stop it
            speechControl.stopListening();
        }
    }
    public boolean hasAudioControl() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String audio_control = sharedPreferences.getString(PreferenceKeys.AudioControlPreferenceKey, "none");
        if( audio_control.equals("voice") ) {
            return speechControl.hasSpeechRecognition();
        }
        else if( audio_control.equals("noise") ) {
            return true;
        }
        return false;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        speechControl = new SpeechControl(this.mainActivity);
        speechControl.initSpeechRecognizer();
        permissionHandler = new PermissionHandler(this.mainActivity);
        if( MyDebug.LOG )
            Log.d(TAG, "clickedAudioControl");
        // check hasAudioControl just in case!
        if( !hasAudioControl() ) {
            if( MyDebug.LOG )
                Log.e(TAG, "clickedAudioControl, but hasAudioControl returns false!");
            return;
        }
        mainActivity.closePopup();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String audio_control = sharedPreferences.getString(PreferenceKeys.AudioControlPreferenceKey, "none");
        if( audio_control.equals("voice") && speechControl.hasSpeechRecognition() ) {
            if( speechControl.isStarted() ) {
                speechControl.stopListening();
            }
            else {
                boolean has_audio_permission = true;
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                    // we restrict the checks to Android 6 or later just in case, see note in LocationSupplier.setupLocationListener()
                    if( MyDebug.LOG )
                        Log.d(TAG, "check for record audio permission");
                    if( ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "record audio permission not available");
                        permissionHandler.requestRecordAudioPermission();
                        has_audio_permission = false;
                    }
                }
                if( has_audio_permission ) {
                    String toast_string = mainActivity.getResources().getString(R.string.speech_recognizer_started) + "\n" +
                            mainActivity.getResources().getString(R.string.speech_recognizer_extra_info);
                    mainActivity.preview.showToast(audio_control_toast, toast_string);
                    speechControl.startSpeechRecognizerIntent();
                    speechControl.speechRecognizerStarted();
                }
            }
        }
        else if( audio_control.equals("noise") ){
            if( audio_listener != null ) {
                freeAudioListener(false);
            }
            else {
                startAudioListener();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainActivity.stopAudioListeners();
    }
}
