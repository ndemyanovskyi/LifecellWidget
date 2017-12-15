package ndemyanovskyi.lifecellwidget.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Account;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;

public class Values {

    public static final String PERMISSIONS_ONCE_REQUESTED = "permissions_one_requested";
    public static final String ACCOUNT_PHONE_NUMBER = "account_phone_number";
    public static final String ACCOUNT_SUPER_PASSWORD = "account_super_password";
    public static final String ACCOUNT_TOKEN = "account_token";
    public static final String ACCOUNT_SUBSCRIPTION_ID = "account_subscription_id";
    //public static final String BALANCES_UPDATE_TIME = "balances_update_time";
    //public static final String INFO_SMS_UPDATE_TIME = "info_sms_update_time";

    private static WeakReference<SharedPreferences> preferencesReference;

    public static SharedPreferences getPreferences(Context context) {
        WeakReference<SharedPreferences> reference = Values.preferencesReference;
        if(reference != null) {
            SharedPreferences preferences = reference.get();
            if(preferences == null) {
                preferences = context.getSharedPreferences(
                        context.getPackageName() + "_values", Context.MODE_PRIVATE);
                preferencesReference = new WeakReference<>(preferences);
            }
            return preferences;
        } else {
            SharedPreferences preferences = context.getSharedPreferences(
                    context.getPackageName() + "_values", Context.MODE_PRIVATE);
            preferencesReference = new WeakReference<>(preferences);
            return preferences;
        }
    }

    /*public static void setBalancesUpdateTime(Context context, LocalDateTime value) {
        setString(context, BALANCES_UPDATE_TIME, value != null ? value.toString() : null);
    }

    public static LocalDateTime getBalancesUpdateTime(Context context) {
        String string = getString(context, BALANCES_UPDATE_TIME);
        return string != null ? LocalDateTime.parse(string) : null;
    }

    public static void setInfoSmsUpdateTime(Context context, LocalDateTime value) {
        setString(context, INFO_SMS_UPDATE_TIME, value != null ? value.toString() : null);
    }

    public static LocalDateTime getInfoSmsUpdateTime(Context context) {
        String string = getString(context, INFO_SMS_UPDATE_TIME);
        return string != null ? LocalDateTime.parse(string) : null;
    }*/

    public static void setAccount(Context context, Account account) {
        setAccountPhoneNumber(context, account != null ? account.getPhoneNumber() : null);
        setString(context, ACCOUNT_SUPER_PASSWORD, account != null ? account.getSuperPassword() : null);
        setString(context, ACCOUNT_TOKEN, account != null ? account.getToken() : null);
        setString(context, ACCOUNT_SUBSCRIPTION_ID, account != null ? account.getSubscriptionId(): null);
    }

    public static Account getAccount(Context context) {
        PhoneNumber phoneNumber = getAccountPhoneNumber(context);
        if(phoneNumber != null) {
            String superPassword = getString(context, ACCOUNT_SUPER_PASSWORD);
            if(superPassword != null) {
                String token = getString(context, ACCOUNT_TOKEN);
                String subscriptionId = getString(context, ACCOUNT_SUBSCRIPTION_ID);
                return new Account(phoneNumber, superPassword, token, subscriptionId);
            }
        }
        return null;
    }

    private static void setAccountPhoneNumber(Context context, PhoneNumber phoneNumber) {
        if(phoneNumber != null) {
            setLong(context, ACCOUNT_PHONE_NUMBER, phoneNumber.toLong());
        } else {
            remove(context, ACCOUNT_PHONE_NUMBER);
        }
    }

    public static PhoneNumber getAccountPhoneNumber(Context context) {
        if(hasAccountPhoneNumber(context)) {
            return PhoneNumber.of(getLong(context, ACCOUNT_PHONE_NUMBER));
        } else {
            return null;
        }
    }

    public static boolean hasAccount(Context context) {
        return hasAccountPhoneNumber(context)
                && has(context, ACCOUNT_SUPER_PASSWORD);
    }

    public static boolean hasAccountPhoneNumber(Context context) {
        return has(context, ACCOUNT_PHONE_NUMBER);
    }

    public static void setPermissionsOnceRequested(Context context, boolean permissionsOnceRequested) {
        setBoolean(context, PERMISSIONS_ONCE_REQUESTED, permissionsOnceRequested);
    }

    public static boolean getPermissionsOnceRequested(Context context) {
        return getBoolean(context, PERMISSIONS_ONCE_REQUESTED);
    }

    private static void setString(Context context, String key, String value) {
        SharedPreferences preferences = getPreferences(context);
        Editor editor = preferences.edit();
        if(value != null) {
            editor.putString(key, value).apply();
        } else {
            editor.remove(key).apply();
        }
    }

    private static String getString(Context context, String key) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getString(key, null);
    }

    private static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences preferences = getPreferences(context);
        Editor editor = preferences.edit();
        editor.putBoolean(key, value).apply();
    }

    private static boolean getBoolean(Context context, String key) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getBoolean(key, false);
    }

    private static void setLong(Context context, String key, long value) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putLong(key, value).apply();
    }

    private static long getLong(Context context, String key) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getLong(key, 0L);
    }

    private static boolean has(Context context, String key) {
        return getPreferences(context).contains(key);
    }

    private static void remove(Context context, String key) {
        getPreferences(context).edit().remove(key).apply();
    }
}
