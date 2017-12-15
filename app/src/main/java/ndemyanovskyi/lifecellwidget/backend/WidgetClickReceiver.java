package ndemyanovskyi.lifecellwidget.backend;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Factor;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Type;

public class WidgetClickReceiver extends BroadcastReceiver {

    public static final String ACTION_CLICK = "ndemyanovskyi.lifecellwidget.backend.action.ACTION_CLICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(ACTION_CLICK.equals(intent.getAction())) {
            handleActionClick(context, intent);
        }
    }

    private void handleActionClick(Context context, Intent intent) {
        if(intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            State state = WidgetProvider.getState(context, appWidgetId);
            if(state != null) {
                Factor factor = state.getFactor();
                Type type = state.getType();
                if (factor != Factor.SMS) {
                    if (type == Type.FAILED && factor != Factor.UPDATE) {
                        UpdateService.userUpdate(context);
                    } else {
                        UpdateService.update(context);
                    }
                }
            } else {
                UpdateService.update(context);
            }
        } else {
            UpdateService.update(context);
        }
    }
}
