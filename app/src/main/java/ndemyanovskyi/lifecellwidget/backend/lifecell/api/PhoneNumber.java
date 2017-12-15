package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumber implements Serializable {

    private static final long MIN_PHONE_VALUE = 100000000000L;
    private static final long MAX_PHONE_VALUE = 999999999999L;

    public static final Pattern PATTERN = Pattern.compile("\\+?([0-9]{12})");

    private final long value;

    private PhoneNumber(long value) {
        this.value = value;
    }

    public static PhoneNumber parse(String text) {
        Matcher matcher = PATTERN.matcher(text);
        if(!matcher.matches()) {
            throw new ParseException(
                    "Illegal phone number: " + text);
        }
        return new PhoneNumber(Long.parseLong(matcher.group(1)));
    }

    public static PhoneNumber of(long value) {
        if(value < MIN_PHONE_VALUE || value > MAX_PHONE_VALUE) {
            throw new IllegalArgumentException(
                    "Value must be only of 11 digits.");
        }
        return new PhoneNumber(value);
    }

    public long toLong() {
        return value;
    }

    @Override
    public String toString() {
        return "+" + value;
    }

    public String toStringWithoutPlus() {
        return Long.toString(value);
    }

    public static class ParseException extends RuntimeException {

        public ParseException() {
        }

        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParseException(Throwable cause) {
            super(cause);
        }
    }
}
