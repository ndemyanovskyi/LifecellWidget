package ndemyanovskyi.lifecellwidget.frontend;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;

import ndemyanovskyi.lifecellwidget.R;
import ndemyanovskyi.lifecellwidget.app.Preferences;
import ndemyanovskyi.lifecellwidget.app.Values;
import ndemyanovskyi.lifecellwidget.backend.ScreenOnOffService;
import ndemyanovskyi.lifecellwidget.backend.UpdateService;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Account;
import ndemyanovskyi.lifecellwidget.frontend.StartActivity.State;
import ndemyanovskyi.lifecellwidget.frontend.util.AppCompatPreferenceActivity;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new GeneralPreferenceFragment()).commit();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener {

        private static final String PREF_BACKGROUND_COLOR = "pref_widget_background_color";
        private static final String PREF_TEXT_COLOR = "pref_widget_text_color";
        private static final String PREF_SIGN_IN = "pref_sign_in";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            setHasOptionsMenu(true);
            findPreference(PREF_SIGN_IN).setOnPreferenceClickListener(this);
            findPreference(PREF_TEXT_COLOR).setOnPreferenceChangeListener(this);
            findPreference(PREF_BACKGROUND_COLOR).setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case PREF_SIGN_IN:
                    Values.setAccount(getContext(), null);
                    ScreenOnOffService.stopIfRunning(getContext());
                    UpdateService.cancelUpdate(getContext());
                    Intent intent = new Intent(getContext(), StartActivity.class);
                    intent.putExtra(StartActivity.EXTRA_STATE, State.SIGN_IN_REQUEST);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case PREF_TEXT_COLOR:
                case PREF_BACKGROUND_COLOR:
                    WidgetProvider.refresh(getContext());
                    return true;
                default:
                    return false;
            }
        }
    }
}
