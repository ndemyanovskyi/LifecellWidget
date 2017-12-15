package ndemyanovskyi.lifecellwidget.backend;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.IOException;
import java.util.Objects;

import ndemyanovskyi.lifecellwidget.app.Permissions;
import ndemyanovskyi.lifecellwidget.app.Values;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Account;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Amounts;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Balances;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.ExpirationDate;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Lifecell;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;
import ndemyanovskyi.lifecellwidget.backend.util.TelephonyUtils;
import ndemyanovskyi.lifecellwidget.frontend.StartActivity;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Factor;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Type;

public class UpdateService extends IntentService {

    public static final String TAG = UpdateService.class.getName();

    private static final int UPDATE_REQUEST_CODE = 1;

    public static final Duration LONG_UPDATE_DURATION = Duration.ofMinutes(40);
    public static final Duration SHORT_UPDATE_DURATION = Duration.ofMinutes(5);

    private static final String ACTION_UPDATE = "ndemyanovskyi.lifecellwidget.backend.action.ACTION_UPDATE";
    private static final String ACTION_USER_UPDATE = "ndemyanovskyi.lifecellwidget.backend.action.ACTION_USER_UPDATE";

    private UpdateIdManager updateIdManager;
    private long updateId;

    public UpdateService() {
        super("UpdateService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateIdManager = UpdateIdManager.from(this);
        updateId = updateIdManager.nextUpdateId();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        State state = WidgetProvider.getState(this);
        if(updateIdManager.currentUpdateId() == updateId
                && state.is(Factor.UPDATE, Type.STARTED)) {
            WidgetProvider.setState(this, new State(Factor.UPDATE, Type.SUCCEED));
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE.equals(action)) {
                handleActionUpdate();
            } else if (ACTION_USER_UPDATE.equals(action)) {
                handleActionUserUpdate();
            }
        }
    }

    private void handleActionUserUpdate() {
        if(!StartActivity.startIfNeeded(this)) {
            handleActionUpdate();
        }
    }

    private void handleActionUpdate() {
        Log.d(TAG, "Updating started.");

        try {
            ScreenOnOffService.startIfStopped(this);

            if(Permissions.allGranted(this)) {
                Account account = Values.getAccount(this);
                if (account != null) {
                    ExpirationDate newestExpirationDate = DatabaseManager
                            .from(this).readNewestExpirationDate(account.getPhoneNumber());
                    if (newestExpirationDate == null
                            || newestExpirationDate.getTime() == null
                            || ChronoUnit.DAYS.between(newestExpirationDate.getTime(), LocalDateTime.now()) >= 1) {
                        requestSms(this);
                    }

                    WidgetProvider.setState(this, new State(Factor.UPDATE, Type.STARTED));
                    try {
                        performSuccess(account.getBalances());
                    } catch (IOException e) {
                        performFailure(e);
                    } catch (LifecellException e) {
                        switch (e.getResponseType()) {
                            case TOKEN_EXPIRED:
                            case SESSION_EXPIRED:
                            case SESSION_TIMEOUT:
                                try {
                                    account = Lifecell.signIn(account);
                                    Values.setAccount(getBaseContext(), account);
                                    performSuccess(account.getBalances());
                                } catch (IOException | LifecellException e1) {
                                    performFailure(e);
                                }
                                break;
                            default:
                                performFailure(e);
                        }
                    }
                } else {
                    Log.w(TAG, "Updating failed: user not logged in.");
                    WidgetProvider.setState(this, new State(Factor.ACCOUNT, Type.FAILED));
                }
            } else {
                Log.w(TAG, "Updating failed: permissions denied.");
                WidgetProvider.setState(this, new State(Factor.PERMISSIONS, Type.FAILED));
            }
        } catch (Throwable ex) {
            Log.e(TAG, "Fatal error whlie processing update: ", ex);
            scheduleNextUpdate(this);
        }
    }

    public static void update(Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.setAction(ACTION_UPDATE);
        context.startService(intent);
    }

    public static void userUpdate(Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.setAction(ACTION_USER_UPDATE);
        context.startService(intent);
    }

    public static void scheduleNextUpdate(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        scheduleNextUpdate(context, powerManager != null && powerManager.isInteractive()
                    ? SHORT_UPDATE_DURATION : LONG_UPDATE_DURATION);
    }

    public static void scheduleNextUpdate(Context context, Duration duration) {
        scheduleNextUpdate(context, duration, false);
    }

    private static void scheduleNextUpdate(Context context, Duration duration, boolean ignorePreviousUpdateTime) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        final PendingIntent updatePendingIntent = createUpdatePendingIntent(context, UPDATE_REQUEST_CODE);

        PhoneNumber phoneNumber = Values.getAccountPhoneNumber(context);
        if(phoneNumber != null) {
            long currentTimeMillis = System.currentTimeMillis();
            long triggerTimeMillis;
            if(!ignorePreviousUpdateTime) {
                final LocalDateTime balancesUpdateTime =
                        DatabaseManager.from(context).readBalancesUpdateTime(phoneNumber);
                if (balancesUpdateTime != null) {
                    long lastUpdateTimeMillis = balancesUpdateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    triggerTimeMillis = currentTimeMillis - (currentTimeMillis - lastUpdateTimeMillis) + duration.toMillis();
                } else {
                    triggerTimeMillis = currentTimeMillis;
                }
            } else {
                triggerTimeMillis = currentTimeMillis + duration.toMillis();
            }
            alarmManager.cancel(updatePendingIntent);
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, updatePendingIntent);

            Duration scheduleDuration = Duration.ofMillis(triggerTimeMillis - currentTimeMillis);
            Log.d(TAG, "Update scheduled after " + scheduleDuration.toMinutes() + " min.");
        } else {
            Log.w(TAG, "Update not scheduled but planned: account doesn`t exists.");
        }
    }

    public static void cancelUpdate(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        final PendingIntent updatePendingIntent = createUpdatePendingIntent(context, UPDATE_REQUEST_CODE);
        alarmManager.cancel(updatePendingIntent);
        Log.d(TAG, "Scheduled update has been cancelled.");
    }

    public static PendingIntent createUpdatePendingIntent(Context context, int requestCode) {
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.setAction(ACTION_UPDATE);
        return PendingIntent.getService(context,
                requestCode, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void requestSms(Context context) {
        if (Permissions.allGranted(context)) {
            PhoneNumber phoneNumber = Values.getAccountPhoneNumber(context);
            if(phoneNumber != null) {
                Integer subscriptionId = TelephonyUtils.getSubscriptionId(context, phoneNumber.toString());
                if (subscriptionId != null) {
                    Intent intent = new Intent(context, InfoSmsReceiver.class);
                    intent.setAction(InfoSmsReceiver.ACTION_SMS_SENT);
                    PendingIntent smsSentPendingIntent = PendingIntent
                            .getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
                    smsManager.sendTextMessage("5016", null, "CHECKBALANCE", smsSentPendingIntent, null);
                    Log.d(TAG, "SMS requested.");
                    WidgetProvider.setState(context, new State(Factor.SMS, Type.STARTED));
                } else {
                    Log.w(TAG, "SMS can`t be requested: no Lifecell sim card found.");
                    WidgetProvider.setState(context, new State(Factor.CARRIER, Type.FAILED));
                }
            } else {
                Log.w(TAG, "SMS can`t be requested: user not logged in.");
                WidgetProvider.setState(context, new State(Factor.ACCOUNT, Type.FAILED));
            }
        } else {
            Log.w(TAG, "SMS can`t be requested: permissions denied.");
            WidgetProvider.setState(context, new State(Factor.PERMISSIONS, Type.FAILED));
        }
    }

    private void performSuccess(Balances balances) {
        Log.d(TAG, "Updating succeed.");

        DatabaseManager databaseManager = DatabaseManager.from(this);
        databaseManager.insertBalances(balances);
        Balances previousBalances = databaseManager.readFirstBalancesToday(balances.getPhoneNumber());
        ExpirationDate expirationDate = databaseManager.readNewestExpirationDate(balances.getPhoneNumber());
        Amounts amounts = Amounts.until(previousBalances, balances, expirationDate);

        State state = new State(Factor.UPDATE, Type.SUCCEED);
        state.putExtra(State.EXTRA_AMOUNTS, amounts);
        WidgetProvider.setState(this, state);
    }

    private void performFailure(Exception e) {
        Log.w(TAG, "Updating failed.", e);

        State state = new State(Factor.UPDATE, Type.FAILED);
        state.putExtra(State.EXTRA_EXCEPTION, e);
        if(e instanceof LifecellException) {
            state.putExtra(State.EXTRA_RESPONSE_TYPE,
                    ((LifecellException) e).getResponseType());
        }
        WidgetProvider.setState(this, state);
        scheduleBackToNormalState();
    }

    private void scheduleBackToNormalState() {
        Handler handler = new Handler(this.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final long currentUpdateId = updateIdManager.currentUpdateId();
                if(currentUpdateId == updateId) {
                    WidgetProvider.setState(UpdateService.this, new State(Factor.UPDATE, Type.SUCCEED));
                }
            }
        }, 3000);
    }

    private static class UpdateIdManager {

        private static final String PREFERENCES_NAME = UpdateIdManager.class.getName() + "_values";
        private static final String PREF_UPDATE_ID = "update_id";

        private final SharedPreferences preferences;

        private UpdateIdManager(SharedPreferences preferences) {
            this.preferences = Objects.requireNonNull(preferences, "preferences");
        }

        public static UpdateIdManager from(Context context) {
            return new UpdateIdManager(context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE));
        }

        public long nextUpdateId() {
            long value = (long) (Math.random() * Long.MAX_VALUE);
            preferences.edit()
                    .putLong(PREF_UPDATE_ID, value)
                    .apply();
            return value;
        }

        public long currentUpdateId() {
            return preferences.getLong(PREF_UPDATE_ID, 0);
        }
    }
}
