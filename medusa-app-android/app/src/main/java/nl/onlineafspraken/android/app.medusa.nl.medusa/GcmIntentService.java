package app.medusa.nl.medusa;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Vibrator;

import app.medusa.nl.medusa.Main;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "Medusa";
    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_ARGS = "payload_args";
    public static final String PROPERTY_USE_SOUND = "sound";
    public static final String PROPERTY_USE_VIBRATE = "vibrate";
    public static final String PROPERTY_USE_NOTIFICATIONS = "notifications";
    public static final String PROPERTY_MUTE_NOTIFICATIONS = "notifications_mute";
    public Handler mHandler;
    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    private SharedPreferences getMySharedPreferences(Context context) {
        return getSharedPreferences(Main.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()) { // has effect of unparcelling Bundle

            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                //sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                //sendNotification("Deleted messages on server: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                final SharedPreferences prefs = getMySharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                if (extras.containsKey("payload")) {
                    Log.i(TAG, "Payload: " + extras.toString());

                    editor.putString(PROPERTY_PAYLOAD, extras.getString("payload"));
                    editor.putString(PROPERTY_PAYLOAD_ARGS, extras.getString("payload_args"));
                    editor.commit();
                }
                else {
                    // Post notification of received message.
                    Log.i(TAG, "Received: " + extras.toString());
                    if(!prefs.getString(PROPERTY_MUTE_NOTIFICATIONS, "0").equals("1")) {
                        if (prefs.getString(PROPERTY_USE_NOTIFICATIONS, "1").equals("1")) {
                            sendNotification(extras.getString("message"));

                            if (prefs.getString(PROPERTY_USE_VIBRATE, "1").equals("1")) {
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(500);
                            }

                            if (prefs.getString(PROPERTY_USE_SOUND, "1").equals("1")) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
                                        mp.start();
                                    }
                                });
                            }
                        }

                        // mute any following notification until app is opened or resumed again
                        editor.putString(PROPERTY_MUTE_NOTIFICATIONS, "1");
                        editor.commit();
                    }
                }
            }
        }
// Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Main.class), 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle("Medusa")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}