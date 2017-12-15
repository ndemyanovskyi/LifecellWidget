package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.threeten.bp.LocalDateTime;

import java.io.Serializable;
import java.util.Objects;

public class Balances implements Serializable {

    public static final long NO_OFFNET = Long.MIN_VALUE;
    public static final long NO_VIDEO = Long.MIN_VALUE;
    public static final long NO_INTERNET = Long.MIN_VALUE;
    public static final double NO_BONUS = Double.MIN_VALUE;
    public static final double NO_MAIN = Double.MIN_VALUE;
    public static final ExpirationDate NO_EXPIRATION_DATE = null;
    public static final LocalDateTime NO_TIMESTAMP = null;

    private final PhoneNumber phoneNumber;
    private final LocalDateTime time;
    private final double main; // hryvnia
    private final double bonus; // hryvnia
    private final long offnet; // seconds
    private final long internet; // bytes
    private final long video; //bytes
    private final ExpirationDate expirationDate;

    public Balances(PhoneNumber phoneNumber, LocalDateTime time, double main, double bonus, long minutesToOtherCarriers,
                    long megabytes3g, long megabytes3gYoutube, ExpirationDate expirationDate) {
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.time = Objects.requireNonNull(time, "time");
        this.main = main;
        this.bonus = bonus;
        this.offnet = minutesToOtherCarriers;
        this.internet = megabytes3g;
        this.video = megabytes3gYoutube;
        this.expirationDate = expirationDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocalDateTime getTime() {
        return time;
    }

    public double getMain() {
        return main;
    }

    public double getBonus() {
        return bonus;
    }

    public long getOffnet() {
        return offnet;
    }

    public long getInternet() {
        return internet;
    }

    public long getVideo() {
        return video;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public boolean hasAmounts() {
        return getVideo() != NO_VIDEO
                && getInternet() != NO_INTERNET
                && getOffnet() != NO_OFFNET
                && getMain() != NO_MAIN
                && getBonus() != NO_BONUS;
    }

    public boolean hasTimings() {
        return getTime() != NO_TIMESTAMP
                && getExpirationDate() != NO_EXPIRATION_DATE;
    }

    public static class Builder {

        private PhoneNumber phoneNumber;
        private LocalDateTime time;
        private double main;
        private double bonus;
        private long offnet;
        private long internet;
        private long video;
        private ExpirationDate expirationDate;

        public void setPhoneNumber(PhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public Builder setBonus(double bonus) {
            this.bonus = bonus;
            return this;
        }

        public Builder setExpirationDate(ExpirationDate expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder setMain(double main) {
            this.main = main;
            return this;
        }

        public Builder setInternet(long internet) {
            this.internet = internet;
            return this;
        }

        public Builder setVideo(long video) {
            this.video = video;
            return this;
        }

        public Builder setOffnet(long offnet) {
            this.offnet = offnet;
            return this;
        }

        public Builder setTime(LocalDateTime timestamp) {
            this.time = timestamp;
            return this;
        }

        public PhoneNumber getPhoneNumber() {
            return phoneNumber;
        }

        public double getBonus() {
            return bonus;
        }

        public double getMain() {
            return main;
        }

        public ExpirationDate getExpirationDate() {
            return expirationDate;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public long getInternet() {
            return internet;
        }

        public long getOffnet() {
            return offnet;
        }

        public long getVideo() {
            return video;
        }

        public boolean hasAmounts() {
            return getVideo() != NO_VIDEO
                    && getInternet() != NO_INTERNET
                    && getOffnet() != NO_OFFNET
                    && getMain() != NO_MAIN
                    && getBonus() != NO_BONUS;
        }

        public boolean hasTimings() {
            return getTime() != NO_TIMESTAMP
                    && getExpirationDate() != NO_EXPIRATION_DATE;
        }

        public Balances build() {
            return new Balances(
                    phoneNumber, time, main, bonus, offnet,
                    internet, video, expirationDate);
        }
    }
}
