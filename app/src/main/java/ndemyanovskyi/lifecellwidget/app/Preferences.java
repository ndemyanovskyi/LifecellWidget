package ndemyanovskyi.lifecellwidget.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import org.threeten.bp.LocalDateTime;

import java.lang.ref.WeakReference;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Account;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;

public class Preferences {

    public static final String WIDGET_BACKGROUND_COLOR = "pref_widget_background_color";
    public static final String WIDGET_TEXT_COLOR = "pref_widget_text_color";

    private static WeakReference<SharedPreferences> preferencesReference;

    private static SharedPreferences getPreferences(Context context) {
        WeakReference<SharedPreferences> reference = Preferences.preferencesReference;
        if(reference != null) {
            SharedPreferences preferences = reference.get();
            if(preferences == null) {
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
                preferencesReference = new WeakReference<>(preferences);
            }
            return preferences;
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferencesReference = new WeakReference<>(preferences);
            return preferences;
        }
    }

    public static int getWidgetBackgroundColor(Context context) {
        return getInt(context, WIDGET_BACKGROUND_COLOR);
    }

    public static int getWidgetTextColor(Context context) {
        return getInt(context, WIDGET_TEXT_COLOR);
    }

    private static void setInt(Context context, String key, int value) {
        SharedPreferences preferences = getPreferences(context);
        Editor editor = preferences.edit();
        editor.putInt(key, value).apply();
    }

    private static int getInt(Context context, String key) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt(key, 0);
    }

    private static boolean has(Context context, String key) {
        return getPreferences(context).contains(key);
    }
}
