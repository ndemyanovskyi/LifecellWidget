package ndemyanovskyi.lifecellwidget.backend.util;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

public class TelephonyUtils {

    public static Integer getSubscriptionId(Context context, String phoneNumber) {
        SubscriptionManager manager = SubscriptionManager.from(context);
        for (SubscriptionInfo info : manager.getActiveSubscriptionInfoList()) {
            if (info.getNumber().contains(phoneNumber)) {
                return info.getSubscriptionId();
            }
        }
        return null;
    }

    public static String getPhoneNumber(Context context, String carrierName) {
        SubscriptionManager manager = SubscriptionManager.from(context);
        for (SubscriptionInfo info : manager.getActiveSubscriptionInfoList()) {
            if (info.getCarrierName().toString().contains(carrierName)) {
                String number = info.getNumber();
                if(number.startsWith("+")) {
                    number.replaceFirst("//+", "");
                }
                return number;
            }
        }
        return null;
    }
}
