package ndemyanovskyi.lifecellwidget.backend;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;


import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ndemyanovskyi.lifecellwidget.app.Values;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.ExpirationDate;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Factor;
import ndemyanovskyi.lifecellwidget.frontend.WidgetProvider.State.Type;

public class InfoSmsReceiver extends BroadcastReceiver {

    public static final String TAG = InfoSmsReceiver.class.getName();

    public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String ACTION_SMS_SENT = "ndemyanovskyi.lifecellwidget.app.backend.InfoSmsReceiver.ACTION_SMS_SENT";

    public static final DateTimeFormatter SMS_EXPIRATION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    private static final String TODAY_EXPIRATION_DATE = "kintsia doby";

    public static final Pattern SMS_PATTERN = Pattern.compile(
                    "Balans ([-+]?[0-9]*\\.?[0-9]+)hrn, " +
                    "bonus ([-+]?[0-9]*\\.?[0-9]+)hrn\\.\\n" +
                            "\\*\\*\\*\\n" +
                            "BEZLIM: dzvinky v lifecell, sots.merezhi\\. " +
                    "(\\d+)hv na inshi merezhi, " +
                    "([-+]?[0-9]*\\.?[0-9]+)GB 3G\\+, " +
                    "([-+]?[0-9]*\\.?[0-9]+)GB video\\. " +
                    "Taryf do ([0-9]{2}\\.[0-9]{2}\\.[0-9]{2}|" + TODAY_EXPIRATION_DATE + ")\\. " +
                    "Nomer do ([0-9]{2}\\.[0-9]{2}\\.[0-9]{2})\\.",
            Pattern.CASE_INSENSITIVE);

    /*
    * Balans 50.00hrn, bonus 0.00hrn.
***
BEZLIM: dzvinky v lifecell, sots.merezhi. 8hv na inshi merezhi, 9.08GB 3G+, 8.0GB video. Taryf do kintsia doby. Nomer do 06.11.18.
    * */

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_SMS_RECEIVED:
                onSmsReceived(context, intent);
                break;
            case ACTION_SMS_SENT:
                int resultCode = getResultCode();
                if(resultCode == Activity.RESULT_OK) {
                    onSmsSentSuccessfully(context, intent);
                } else {
                    onSmsSentFailed(context, intent, resultCode);
                }
        }
    }

    private static void onSmsSentSuccessfully(Context context, Intent intent) {
        Log.d(TAG, "SMS sent successfully.");
        //WidgetProvider.update(context, new State(Factor.SMS, Type.SENT));
    }

    private static void onSmsSentFailed(final Context context, Intent intent, int resultCode) {
        Log.d(TAG, "SMS sent failed. Result code = " + resultCode);
        WidgetProvider.setState(context, new State(Factor.SMS, Type.FAILED));
    }

    private static void onSmsReceived(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        if (bundle != null) {
            final byte[][] pdus = (byte[][]) bundle.get("pdus");
            if(pdus != null) {
                final PhoneNumber phoneNumber = Values.getAccountPhoneNumber(context);
                if(phoneNumber != null) {
                    Map<LocalDateTime, StringBuilder> messages = new HashMap<>();
                    for (byte[] pdu : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu(pdu, "3gpp");
                        if (sms.getDisplayOriginatingAddress().equals("5433")) {
                            LocalDateTime timestamp = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(sms.getTimestampMillis()), ZoneId.systemDefault());
                            StringBuilder builder = messages.get(timestamp);
                            if (builder == null) {
                                builder = new StringBuilder();
                                messages.put(timestamp, builder);
                            }
                            builder.append(sms.getDisplayMessageBody());
                        }
                    }
                    for (Entry<LocalDateTime, StringBuilder> message : messages.entrySet()) {
                        LocalDateTime timestamp = message.getKey();
                        String sms = message.getValue().toString();
                        Log.d(TAG, "SMS received: " + sms);
                        try {
                            ExpirationDate expirationDate = parseExpirationDate(phoneNumber, timestamp, sms);
                            DatabaseManager.from(context).insertExpirationDate(expirationDate);
                            State state = new State(Factor.SMS, Type.SUCCEED);
                            state.putExtra(State.EXTRA_EXPIRATION_DATE, expirationDate);
                            WidgetProvider.setState(context, state);
                        } catch (SmsParseException | SQLException ex) {
                            Log.w(TAG, ex);
                        }
                    }
                }
            }
        }
    }

    public static ExpirationDate parseExpirationDate(PhoneNumber phoneNumber, LocalDateTime time, String sms) {
        Matcher matcher = SMS_PATTERN.matcher(sms);
        if(!matcher.find()) {
            throw new SmsParseException(
                    "Text does not contains expiration date information: " + sms);
        }
        try {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println(matcher.group(i));
            }
            String date = matcher.group(6);
            LocalDate expirationDate = !date.equals(TODAY_EXPIRATION_DATE)
                    ? LocalDate.parse(date, SMS_EXPIRATION_DATE_FORMATTER)
                    : LocalDate.now();
            return new ExpirationDate(phoneNumber, time, expirationDate);
        } catch (DateTimeParseException ex) {
            throw new SmsParseException(
                    "Problems with sms of pattern: " + sms, ex);
        }
    }

    public static class SmsParseException extends RuntimeException {

        public SmsParseException() {
        }

        public SmsParseException(String message) {
            super(message);
        }

        public SmsParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public SmsParseException(Throwable cause) {
            super(cause);
        }
    }
}
