package com.tanayjoshi.android.screenrecordingapp;

import static android.app.Activity.RESULT_OK;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.os.Environment.DIRECTORY_MOVIES;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;

public final class RecordService extends Service {

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private int resultCode;
    private Intent data;
//    private BroadcastReceiver mScreenStateReceiver;

    private static final String TAG = "RECORDERSERVICE";
    private static final String EXTRA_RESULT_CODE = "resultcode";
    private static final String EXTRA_DATA = "data";
    private static final int ONGOING_NOTIFICATION_ID = 23;


    @Override
    public void onCreate() {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Starting recording service", Toast.LENGTH_SHORT).show();

        resultCode = intent.getIntExtra("resultCode", 0);
        data = intent.getParcelableExtra(EXTRA_DATA);

        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }

        final String channelId = "channel";
        NotificationChannel channel= new NotificationChannel(channelId,channelId, NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle("DataRecorder")
                        .setContentText("Your screen is being recorded and saved to your phone.")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setChannelId(channelId)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        Log.e("run: ", "hello");
        if (resultCode == RESULT_OK) {
            startRecording(resultCode, data);
        }


        return START_REDELIVER_INTENT;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startRecording(int resultCode, Intent data) {
        Log.e( "startRecording: ","hello" );
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService (Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);

        int mScreenDensity = metrics.densityDpi;
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        String videoDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES).getAbsolutePath();
        Long timestamp = System.currentTimeMillis();
        String filePathAndName = videoDir + "/time_" + timestamp.toString() + ".mp4";

        mMediaRecorder.setOutputFile(filePathAndName);
        mMediaRecorder.setVideoSize(displayWidth, displayHeight);
        mMediaRecorder.setVideoFrameRate(15);
        mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000);


        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        Surface surface = mMediaRecorder.getSurface();
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainActivity",
                displayWidth, displayHeight, mScreenDensity, VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface, null, null);
        mMediaRecorder.start();

        Log.e(TAG, "Started recording");
    }

    private void stopRecording() {
        Log.e("hello", "stopRecording: ");
        mMediaRecorder.stop();
        mMediaProjection.stop();
        mMediaRecorder.release();
        mVirtualDisplay.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        stopSelf();
        Toast.makeText(this, "Recorder service stopped", Toast.LENGTH_SHORT).show();
    }
}
