package ndemyanovskyi.lifecellwidget.app;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

public class Permissions {

    public static final String[] ARRAY = {
            permission.READ_PHONE_STATE,
            permission.RECEIVE_SMS,
            permission.READ_SMS,
            permission.SEND_SMS
    };

    public static boolean allGranted(Context context) {
        for (String permission : ARRAY) {
            if(context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static State state(Activity activity) {
        State state = State.GRANTED;
        for (String permission : ARRAY) {
            if(activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                if(activity.shouldShowRequestPermissionRationale(permission)) {
                    state = State.DENIED;
                } else {
                    if(Values.getPermissionsOnceRequested(activity)) {
                        return State.FOREVER_DENIED;
                    } else {
                        return State.DENIED;
                    }
                }
            }
        }
        return state;
    }

    public enum State {
        GRANTED, DENIED, FOREVER_DENIED
    }

}
