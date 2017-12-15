package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.io.Serializable;
import java.util.Objects;

public class ExpirationDate implements Serializable {

    private final PhoneNumber phoneNumber;
    private final LocalDateTime time;
    private final LocalDate value;

    public ExpirationDate(PhoneNumber phoneNumber, LocalDateTime time, LocalDate value) {
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.time = time;
        this.value = Objects.requireNonNull(value, "value");
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getValue() {
        return value;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
