package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import android.content.Context;
import android.content.res.Resources;

import ndemyanovskyi.lifecellwidget.R;

public class LifecellException extends Exception {

    private final ResponseType responseType;
    private final long responseCode;

    public LifecellException(ResponseType responseType) {
        this(responseType, null);
    }

    public LifecellException(String message, ResponseType responseType) {
        super(message);
        this.responseType = responseType;
        this.responseCode = responseType.getResponseCode();
    }

    public LifecellException(String message, ResponseType responseType, Throwable cause) {
        this(message, responseType, responseType.getResponseCode(), null);
    }

    public LifecellException(ResponseType responseType, Throwable cause) {
        this(responseType, responseType.getResponseCode(), cause);
    }

    public LifecellException(ResponseType responseType, long responseCode) {
        this(responseType, responseCode, null);
    }

    public LifecellException(String message, ResponseType responseType, long responseCode) {
        this(message, responseType, responseCode, null);
    }

    public LifecellException(String message, ResponseType responseType, long responseCode, Throwable cause) {
        super(message, cause);
        this.responseType = responseType;
        this.responseCode = responseCode;
    }

    public LifecellException(ResponseType responseType, long responseCode, Throwable cause) {
        this("Response type = " + responseType.name() + ", response code = " + responseCode,
                responseType, responseCode, cause);
    }

    public long getResponseCode() {
        return responseCode;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public enum ResponseType {

        SUCCESS(0L, Type.SUCCESS),

        SESSION_TIMEOUT(-1L, Type.ERROR),
        INTERNAL_ERROR(-2L, Type.ERROR),
        INVALID_PARAMETER_LIST(-3L, Type.ERROR),
        AUTHORIZATION_FAILED(-4L, Type.ERROR),
        TOKEN_EXPIRED(-5L, Type.ERROR),
        AUTHORIZATION_FAILED_WRONG_LINK(-6L, Type.ERROR),
        WRONG_SUPERPASSWORD(-7L, Type.ERROR),
        WRONG_NUMBER(-8L, Type.ERROR),
        ONLY_FOR_PREPAID_CUSTOMERS(-9L, Type.ERROR),
        SUPERPASSWORD_LOCKED(-10L, Type.ERROR),
        NUMBER_DOESNT_EXISTS(-11L, Type.ERROR),
        SESSION_EXPIRED(-12L, Type.ERROR),
        TARIFF_PLAN_CHANGING_ERROR(-13L, Type.ERROR),
        SERVICE_ACTIVATION_ERROR(-14L, Type.ERROR),
        ORDER_ACTIVATION_ERROR(-15L, Type.ERROR),
        FAILED_TO_GET_THE_LIST_OF_TARIFFS(-16L, Type.ERROR),
        FAILED_TO_GET_THE_LIST_OF_SERVICES(-17L, Type.ERROR),
        REMOVE_SERVICE_FROM_PREPROCESSING_FAILED(-18L, Type.ERROR),
        LOGIC_IS_BLOCKED(-19L, Type.ERROR),
        TOO_MANY_REQUESTS(-20L, Type.ERROR),
        PAYMENTS_OF_EXPENSES_MISSED(-40L, Type.ERROR),
        INTERNAL_APPLICATION_ERROR(-21474833648L, Type.ERROR),

        //Client error codes
        RESPONSE_STRUCTURE_CHANGED(1L, Type.ERROR),
        INVALID_SUPERPASSWORD(2L, Type.ERROR),
        INVALID_NUMBER(3L, Type.ERROR),
        OTHER_CARRIER_NUMBER(4L, Type.ERROR),

        //Common error codes
        UNKNOWN(null, Type.UNKNOWN),
        IO_ERROR(5L, Type.ERROR);

        private Long responseCode;
        private String message;
        private Type type;

        ResponseType(Long responseCode, Type type) {
            this.responseCode = responseCode;
            this.message = message;
            this.type = type;
        }

        public static ResponseType ofResponseCode(long responseCode) {
            for (ResponseType responseType: ResponseType.values()) {
                Long id = responseType.getResponseCode();
                if (id != null && id == responseCode) {
                    return responseType;
                }
            }
            return UNKNOWN;
        }

        public Type getType() {
            return type;
        }

        public Long getResponseCode() {
            return responseCode;
        }

        /*public String getMessage() {
            return message;
        }*/

        public enum Type {
            SUCCESS, ERROR, UNKNOWN
        }

        public CharSequence getMessage(Context context) {
            int resId = context.getResources().getIdentifier(
                    "response_type_message_" + name(), "string", context.getPackageName());
            if(resId == 0) {
                throw new IllegalArgumentException(
                        "No message text for response type " + name());
            }
            return context.getText(resId);
        }

        public CharSequence getMessage(Context context, CharSequence defaultValue) {
            int resId = context.getResources().getIdentifier(
                    "response_type_message_" + name(), "string", context.getPackageName());
            return resId != 0 ? context.getText(resId) : defaultValue;
        }

        public CharSequence getMessage(Context context, int defaultValueResId) {
            int resId = context.getResources().getIdentifier(
                    "response_type_message_" + name(), "string", context.getPackageName());
            return context.getText(resId != 0 ? resId : defaultValueResId);
        }
    }
}
