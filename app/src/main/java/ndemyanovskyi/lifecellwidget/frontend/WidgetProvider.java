package ndemyanovskyi.lifecellwidget.frontend;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ndemyanovskyi.lifecellwidget.R;
import ndemyanovskyi.lifecellwidget.app.Permissions;
import ndemyanovskyi.lifecellwidget.app.Preferences;
import ndemyanovskyi.lifecellwidget.app.Values;
import ndemyanovskyi.lifecellwidget.backend.WidgetClickReceiver;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Amounts;
import ndemyanovskyi.lifecellwidget.backend.DatabaseManager;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Balances;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.ExpirationDate;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Factor;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Type;

public class WidgetProvider extends AppWidgetProvider {

    public static final String TAG = WidgetProvider.class.getName();

    public static final String EXTRA_STATE = "state";

    private static final String PREFERENCES_NAME = WidgetProvider.class.getName() + "_values";

    public static final DateTimeFormatter EXPIRATION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter UPDATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        onUpdate(context, appWidgetManager, appWidgetIds, createDefaultState(context));
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, State state) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        onUpdateInternal(context, appWidgetManager, appWidgetIds, state);
    }

    private void onUpdateInternal(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, State state) {
        Log.d(TAG, "Widget updating to state: " + state);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);

            Amounts amounts = state.getExtra(State.EXTRA_AMOUNTS);
            if (amounts != null) {
                updateAmounts(context, views, amounts);
                updateProgressBars(views, amounts);
            }
            ExpirationDate expirationDate = state.getExtra(State.EXTRA_EXPIRATION_DATE);
            if(expirationDate != null) {
                updateExpirationDateOrHide(context, views, expirationDate);
            }
            updateColors(context, views);
            updateFooter(context, views, state, amounts, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private static void updateFooter(Context context, RemoteViews views, State state, Amounts amounts, int appWidgetId) {
        views.setOnClickPendingIntent(R.id.layout_footer, createWidgetClickPendingIntent(context, appWidgetId));

        if(state.getType() == Type.STARTED && state.getFactor() == Factor.UPDATE) {
            views.setDisplayedChild(R.id.viewflipper_state, 0);
            views.setDisplayedChild(R.id.viewflipper_message, 1);
            views.setViewVisibility(R.id.textview_expiration_date_message, View.VISIBLE);
        } else if(state.getType() == Type.SUCCEED && state.getFactor() == Factor.UPDATE) {
            if(amounts != null) {
                LocalDateTime time = amounts.getTime();
                views.setTextViewText(R.id.textview_update_time,
                        time != null ? time.format(UPDATE_TIME_FORMATTER) : "");
            }
            views.setDisplayedChild(R.id.viewflipper_state, 0);
            views.setDisplayedChild(R.id.viewflipper_message, 0);
            views.setViewVisibility(R.id.textview_expiration_date_message, View.VISIBLE);
        } else if(state.getType() == Type.FAILED) {
            CharSequence message;
            switch (state.getFactor()) {
                case ACCOUNT:
                    message = context.getString(R.string.message_not_logged_in);
                    break;
                case CARRIER:
                    message = context.getString(R.string.message_carrier_not_found);
                    break;
                case PERMISSIONS:
                    message = context.getString(R.string.message_permissions_denied);
                    break;
                case UPDATE:
                    ResponseType responseType = state.getExtra(State.EXTRA_RESPONSE_TYPE);
                    if(responseType != null) {
                        message = responseType.getMessage(context, R.string.message_connection_error);
                    } else {
                        message = context.getString(R.string.message_connection_error);
                    }
                    break;
                default:
                    message = context.getString(R.string.message_unknown_error);
                    break;
            }
            views.setTextViewText(R.id.textview_failure_message, message);
            views.setDisplayedChild(R.id.viewflipper_state, 1);
            views.setDisplayedChild(R.id.viewflipper_message, 2);
            views.setViewVisibility(R.id.textview_expiration_date_message, View.GONE);
        }
    }

    private static void updateProgressBars(RemoteViews views, Amounts amounts) {
        int internetProgress;
        int internetSecondaryProgress;
        final int internetMinProgress = 0;
        final int internetMaxProgress = 100;

        int videoProgress;
        int videoSecondaryProgress;
        final int videoMinProgress = 0;
        final int videoMaxProgress = 100;

        if (amounts.getInternetRecommendedToday() != Amounts.NO_AMOUNT) {
            if (amounts.getInternetRecommendedToday() > amounts.getInternetUsedToday()) {
                internetProgress = (int) Math.round(((double) amounts.getInternetUsedToday())
                        / ((double) amounts.getInternetRecommendedToday()) * internetMaxProgress);
                internetSecondaryProgress = internetMinProgress;
            } else {
                internetProgress = (int) Math.round(((double) amounts.getInternetRecommendedToday())
                        / ((double) amounts.getInternetUsedToday()) * internetMaxProgress);
                internetSecondaryProgress = internetMaxProgress;
            }
        } else {
            internetProgress = internetMaxProgress;
            internetSecondaryProgress = internetMinProgress;
        }
        if (amounts.getVideoRecommendedToday() != Amounts.NO_AMOUNT) {
            if (amounts.getVideoRecommendedToday() > amounts.getVideoUsedToday()) {
                videoProgress = (int) Math.round(((double) amounts.getVideoUsedToday())
                        / ((double) amounts.getVideoRecommendedToday()) * videoMaxProgress);
                videoSecondaryProgress = videoMinProgress;
            } else {
                videoProgress = (int) Math.round(((double) amounts.getVideoRecommendedToday())
                        / ((double) amounts.getVideoUsedToday()) * videoMaxProgress);
                videoSecondaryProgress = videoMaxProgress;
            }
        } else {
            videoProgress = videoMaxProgress;
            videoSecondaryProgress = videoMinProgress;
        }

        views.setInt(R.id.progressbar_internet, "setProgress", internetProgress);
        views.setInt(R.id.progressbar_internet, "setSecondaryProgress", internetSecondaryProgress);

        views.setInt(R.id.progressbar_video, "setProgress", videoProgress);
        views.setInt(R.id.progressbar_video, "setSecondaryProgress", videoSecondaryProgress);
    }

    private static void updateColors(Context context, RemoteViews views) {
        final int backgroundColor = Preferences.getWidgetBackgroundColor(context);
        final int textColor = Preferences.getWidgetTextColor(context);

        views.setInt(R.id.layout_content, "setBackgroundColor", backgroundColor);
        views.setInt(R.id.layout_bottom_divider, "setBackgroundColor", textColor);
        views.setInt(R.id.layout_center_divider, "setBackgroundColor", textColor);

        views.setInt(R.id.imageview_refresh, "setColorFilter", textColor);
        views.setInt(R.id.imageview_failure, "setColorFilter", textColor);

        views.setInt(R.id.textview_internet_total_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_used_today_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_remaining_today_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_recommended_today_amount, "setTextColor", textColor);

        views.setInt(R.id.textview_internet_total_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_used_today_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_remaining_today_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_internet_recommended_today_amount_unit, "setTextColor", textColor);

        views.setInt(R.id.textview_video_total_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_video_used_today_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_video_remaining_today_amount, "setTextColor", textColor);
        views.setInt(R.id.textview_video_recommended_today_amount, "setTextColor", textColor);

        views.setInt(R.id.textview_video_total_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_video_used_today_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_video_remaining_today_amount_unit, "setTextColor", textColor);
        views.setInt(R.id.textview_video_recommended_today_amount_unit, "setTextColor", textColor);

        views.setInt(R.id.textview_label_video, "setTextColor", textColor);
        views.setInt(R.id.textview_label_internet, "setTextColor", textColor);

        views.setInt(R.id.textview_update_time, "setTextColor", textColor);
        views.setInt(R.id.textview_failure_message, "setTextColor", textColor);
        views.setInt(R.id.textview_progress_message, "setTextColor", textColor);
        views.setInt(R.id.textview_expiration_date_message, "setTextColor", textColor);
    }

    private static void updateAmounts(Context context, RemoteViews views, Amounts amounts) {
        updateAmount(context, views, R.id.textview_internet_total_amount, amounts.getInternetRemainingTotal());
        updateAmount(context, views, R.id.textview_internet_used_today_amount, amounts.getInternetUsedToday());
        updateAmountOrHide(views, R.id.textview_internet_remaining_today_amount,
                R.id.layout_internet_remaining_today_amount, amounts.getInternetRemainingToday());
        updateAmountOrHide(views, R.id.textview_internet_recommended_today_amount,
                R.id.layout_internet_recommended_today_amount, amounts.getInternetRecommendedToday());
        updateAmount(context, views, R.id.textview_video_total_amount, amounts.getVideoRemainingTotal());
        updateAmount(context, views, R.id.textview_video_used_today_amount, amounts.getVideoUsedToday());
        updateAmountOrHide(views, R.id.textview_video_remaining_today_amount,
                R.id.layout_video_remaining_today_amount, amounts.getVideoRemainingToday());
        updateAmountOrHide(views, R.id.textview_video_recommended_today_amount,
                R.id.layout_video_recommended_today_amount, amounts.getVideoRecommendedToday());
        updateExpirationDateOrHide(context, views, amounts.getExpirationDate());
    }

    private static PendingIntent createWidgetClickPendingIntent(Context context, int appWidgetId) {
        Intent intent = new Intent(context, WidgetClickReceiver.class);
        intent.setAction(WidgetClickReceiver.ACTION_CLICK);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void updateExpirationDateOrHide(Context context, RemoteViews views, ExpirationDate expirationDate) {
        if(expirationDate != null) {
            String text = context.getString(
                    R.string.tariff_expiration_date_message, expirationDate.getValue().format(EXPIRATION_DATE_FORMATTER));
            views.setTextViewText(R.id.textview_expiration_date_message, text);
        } else {
            views.setViewVisibility(R.id.textview_expiration_date_message, View.GONE);
        }
    }

    private static void updateAmount(Context context, RemoteViews views, int textViewResId, long amount) {
        String text = amount != Amounts.NO_AMOUNT ? Long.toString(amount / 1024 / 1024) : context.getString(R.string.no_data);
        views.setTextViewText(textViewResId, text);
    }

    private static void updateAmountOrHide(RemoteViews views, int textViewResId, int parentViewResId, long amount) {
        if(amount != Amounts.NO_AMOUNT) {
            views.setViewVisibility(parentViewResId, View.VISIBLE);
            views.setTextViewText(textViewResId, Long.toString(amount / 1024 / 1024));
        } else {
            views.setViewVisibility(parentViewResId, View.GONE);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Objects.equals(intent.getAction(), AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                && intent.hasExtra(EXTRA_STATE)
                && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetIds[] = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            State state = intent.getParcelableExtra(EXTRA_STATE);
            onUpdate(context, appWidgetManager, appWidgetIds, state);
        } else {
            super.onReceive(context, intent);
        }
    }

    public static State createDefaultState(Context context) {
        if(Permissions.allGranted(context)) {
            State state = new State(Factor.UPDATE, Type.SUCCEED);
            PhoneNumber phoneNumber = Values.getAccountPhoneNumber(context);
            if(phoneNumber != null) {
                DatabaseManager databaseManager = DatabaseManager.from(context);
                Balances currentBalances = databaseManager.readNewestBalances(phoneNumber);
                if(currentBalances != null) {
                    Balances previousBalances = databaseManager.readFirstBalancesToday(phoneNumber);
                    Amounts amounts = Amounts.until(previousBalances, currentBalances);
                    state.putExtra(State.EXTRA_AMOUNTS, amounts);
                }
            }
            return state;
        } else {
            return new State(Factor.PERMISSIONS, Type.FAILED);
        }
    }

    public static State getState(Context context, int appWidgetId) {
        State state = StateManager.getState(context, appWidgetId);
        return state != null ? state : new State(Factor.UPDATE, Type.SUCCEED);
    }

    public static State getState(Context context, int appWidgetId) {
        State state = StateManager.getState(context, appWidgetId);
        return state != null ? state : new State(Factor.UPDATE, Type.SUCCEED);
    }

    public static void refresh(Context context) {
        setState(context, createDefaultState(context));
    }

    public static void setState(Context context, State state) {
        int appWidgetIds[] = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, WidgetProvider.class));

        StateManager.setState(context, appWidgetIds, state);
        Intent intent = new Intent(context, WidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtra(WidgetProvider.EXTRA_STATE, state);
        context.sendBroadcast(intent);
    }

    public static class State implements Parcelable {

        public static final Creator<State> CREATOR = new StateCreator();

        public static final String EXTRA_AMOUNTS = "amounts";
        public static final String EXTRA_EXPIRATION_DATE = "tariff_expiration_date";
        public static final String EXTRA_EXCEPTION = "exception";
        public static final String EXTRA_RESPONSE_TYPE = "response_type";

        private final Map<String, Object> extras;
        private final Factor factor;
        private final Type type;

        public State(Factor factor, Type type) {
            this(factor, type, new HashMap<String, Object>());
        }

        private State(Factor factor, Type type, Map<String, Object> extras) {
            this.factor = Objects.requireNonNull(factor, "factor");
            this.type = Objects.requireNonNull(type, "type");
            this.extras = extras;
        }

        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key) {
            return (T) extras.get(key);
        }

        public boolean hasExtra(String key) {
            return extras.containsKey(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key, T defaultValue) {
            if(extras.containsKey(key)) {
                return (T) extras.get(key);
            } else {
                return defaultValue;
            }
        }

        public <S extends Serializable> void putExtra(String key, S value) {
            extras.put(key, value);
        }

        public <P extends Parcelable> void putExtra(String key, P value) {
            extras.put(key, value);
        }

        public Type getType() {
            return type;
        }

        public Factor getFactor() {
            return factor;
        }

        public boolean is(Factor factor, Type type) {
            return getFactor() == factor && getType() == type;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeSerializable(factor);
            dest.writeSerializable(type);
            dest.writeMap(extras);
        }

        @Override
        public String toString() {
            return "State {type=" + type + "; factor=" + factor + "; extras=" + extras + "}";
        }

        public static class StateCreator implements Creator<State> {

            @Override
            @SuppressWarnings("unchecked")
            public State createFromParcel(Parcel source) {
                Factor factor = (Factor) source.readSerializable();
                Type type = (Type) source.readSerializable();
                HashMap extras = source.readHashMap(getClass().getClassLoader());
                return new State(factor, type, extras);
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        }

        public enum Type {
            STARTED,
            FAILED,
            SUCCEED
        }

        public enum Factor {
            ACCOUNT, // FAILED
            CARRIER, // FAILED
            PERMISSIONS, // FAILED, SUCCEED
            SMS, // STARTED, FAILED, SUCCEED
            UPDATE // STARTED, FAILED, SUCCEED
        }
    }

    private static class StateManager {

        private static final String PREFERENCES_NAME = StateManager.class.getName() + "_values";

        private static final String PREF_TYPE_PREFIX = "type_";
        private static final String PREF_FACTOR_PREFIX = "factor_";

        private static WeakReference<SharedPreferences> preferencesReference;

        private static SharedPreferences getPreferences(Context context) {
            WeakReference<SharedPreferences> reference = preferencesReference;
            if(reference != null) {
                SharedPreferences preferences = reference.get();
                if(preferences == null) {
                    preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                    preferencesReference = new WeakReference<>(preferences);
                }
                return preferences;
            } else {
                SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                preferencesReference = new WeakReference<>(preferences);
                return preferences;
            }
        }

        public static State getState(Context context, int appWidgetId) {
            SharedPreferences preferences = getPreferences(context);
            String stateTypeString = preferences.getString(PREF_TYPE_PREFIX + appWidgetId, null);
            if(stateTypeString != null) {
                Type stateType = Type.valueOf(stateTypeString);
                String stateFactorString = preferences.getString(PREF_FACTOR_PREFIX + appWidgetId, null);
                if(stateFactorString != null) {
                    Factor stateFactor = Factor.valueOf(stateFactorString);
                    return new State(stateFactor, stateType);
                }
            }
            return null;
        }

        public static void setState(Context context, int appWidgetId, State state) {
            SharedPreferences preferences = getPreferences(context);
            preferences.edit()
                    .putString(PREF_FACTOR_PREFIX + appWidgetId, state.getFactor().name())
                    .putString(PREF_TYPE_PREFIX + appWidgetId, state.getType().name())
                    .apply();
        }

        public static void setState(Context context, int[] appWidgetIds, State state) {
            for (int appWidgetId : appWidgetIds) {
                setState(context, appWidgetIds, state);
            }
        }

    }
}
