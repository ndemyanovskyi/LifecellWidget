package ndemyanovskyi.lifecellwidget.backend;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import ndemyanovskyi.lifecellwidget.app.Application;

public class ScreenOnOffService extends Service {

    private static final String TAG = ScreenOnOffReceiver.class.getName();

    private ScreenOnOffReceiver screenOnOffReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service created.");
        registerScreenStatusReceiver();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed.");
        unregisterScreenStatusReceiver();
    }

    public static boolean startIfStopped(Context context) {
        Application application = (Application) context.getApplicationContext();

        if(!application.isServiceRunning(ScreenOnOffService.class)) {
            Intent intent = new Intent(application, ScreenOnOffService.class);
            application.startService(intent);
            return true;
        } else {
            return false;
        }
    }

    public static boolean stopIfRunning(Context context) {
        Application application = (Application) context.getApplicationContext();

        if(!application.isServiceRunning(ScreenOnOffService.class)) {
            Intent intent = new Intent(application, ScreenOnOffService.class);
            application.stopService(intent);
            return true;
        } else {
            return false;
        }
    }

    private void registerScreenStatusReceiver() {
        screenOnOffReceiver = new ScreenOnOffReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenOnOffReceiver, filter);
    }

    private void unregisterScreenStatusReceiver() {
        try {
            if (screenOnOffReceiver != null) {
                unregisterReceiver(screenOnOffReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Problem with unregistration receiver.", e);
        }
    }

    public class ScreenOnOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                UpdateService.scheduleNextUpdate(context, UpdateService.LONG_UPDATE_DURATION);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                UpdateService.scheduleNextUpdate(context, UpdateService.SHORT_UPDATE_DURATION);
            }
        }
    }
}
