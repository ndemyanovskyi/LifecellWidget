package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.Serializable;

public class Amounts implements Serializable {

    public static final long INTERNET_TOTAL = 10L * 1024 * 1024 * 1024;
    public static final long VIDEO_TOTAL = 10L * 1024 * 1024 * 1024;

    public static final long NO_AMOUNT = -1;

    private final LocalDateTime time;
    private final ExpirationDate expirationDate;
    private final long internetTotal = INTERNET_TOTAL;
    private final long internetUsedTotal;
    private final long internetRemainingTotal;
    private final long internetUsedToday;
    private final long internetRemainingToday;
    private final long internetRecommendedToday;
    private final long videoTotal = VIDEO_TOTAL;
    private final long videoUsedTotal;
    private final long videoRemainingTotal;
    private final long videoUsedToday;
    private final long videoRemainingToday;
    private final long videoRecommendedToday;

    public Amounts(LocalDateTime time, ExpirationDate expirationDate, long internetRemainingTotal, long internetUsedToday, long internetRemainingToday,
                   long internetRecommendedToday, long videoRemainingTotal, long videoUsedToday,
                   long videoRemainingToday, long videoRecommendedToday) {
        this.time = time;
        this.expirationDate = expirationDate;
        this.internetUsedTotal = internetTotal - internetRemainingTotal;
        this.internetRemainingTotal = internetRemainingTotal;
        this.internetUsedToday = internetUsedToday;
        this.internetRemainingToday = internetRemainingToday;
        this.internetRecommendedToday = internetRecommendedToday;
        this.videoUsedTotal = videoTotal - videoRemainingTotal;
        this.videoRemainingTotal = videoRemainingTotal;
        this.videoUsedToday = videoUsedToday;
        this.videoRemainingToday = videoRemainingToday;
        this.videoRecommendedToday = videoRecommendedToday;
    }

    public static Amounts until(Balances previousBalances, Balances currentBalances) {
        return until(previousBalances, currentBalances, currentBalances.getExpirationDate());
    }

    public static Amounts until(Balances previousBalances, Balances currentBalances, ExpirationDate expirationDate) {
        LocalDateTime time = currentBalances.getTime();
        long internetRemainingTotal = currentBalances.getInternet();
        long videoRemainingTotal = currentBalances.getVideo();
        long internetUsedToday = Amounts.NO_AMOUNT;
        long internetRemainingToday = Amounts.NO_AMOUNT;
        long internetRecommendedToday = Amounts.NO_AMOUNT;
        long videoUsedToday = Amounts.NO_AMOUNT;
        long videoRemainingToday = Amounts.NO_AMOUNT;
        long videoRecommendedToday = Amounts.NO_AMOUNT;

        if (previousBalances != null) {
            internetUsedToday = Math.max(previousBalances.getInternet() - internetRemainingTotal, Amounts.NO_AMOUNT);
            videoUsedToday = Math.max(previousBalances.getVideo() - videoRemainingTotal, Amounts.NO_AMOUNT);

            if(expirationDate != null) {
                long expirationDuration = LocalDate.now().until(expirationDate.getValue(), ChronoUnit.DAYS) + 1;
                if (expirationDuration > 0) {
                    internetRecommendedToday = internetRemainingTotal / expirationDuration;
                    videoRecommendedToday = videoRemainingTotal / expirationDuration;
                    if (internetUsedToday != Amounts.NO_AMOUNT) {
                        internetRemainingToday = Math.max(internetRecommendedToday - internetUsedToday, Amounts.NO_AMOUNT);
                    }
                    if (videoUsedToday != Amounts.NO_AMOUNT) {
                        videoRemainingToday = Math.max(videoRecommendedToday - videoUsedToday, Amounts.NO_AMOUNT);
                    }
                }
            }
        }
        return new Amounts(
                time, expirationDate, internetRemainingTotal, internetUsedToday, internetRemainingToday,
                internetRecommendedToday, videoRemainingTotal, videoUsedToday,
                videoRemainingToday, videoRecommendedToday);
    }

    public LocalDateTime getTime() {
        return time;
    }

    public ExpirationDate getExpirationDate() {
        return expirationDate;
    }

    public long getInternetRemainingTotal() {
        return internetRemainingTotal;
    }

    public long getInternetUsedToday() {
        return internetUsedToday;
    }

    public long getInternetRemainingToday() {
        return internetRemainingToday;
    }

    public long getInternetRecommendedToday() {
        return internetRecommendedToday;
    }

    public long getVideoRemainingTotal() {
        return videoRemainingTotal;
    }

    public long getVideoUsedToday() {
        return videoUsedToday;
    }

    public long getVideoRemainingToday() {
        return videoRemainingToday;
    }

    public long getVideoRecommendedToday() {
        return videoRecommendedToday;
    }

    public long getInternetUsedTotal() {
        return internetUsedTotal;
    }

    public long getInternetTotal() {
        return internetTotal;
    }

    public long getVideoTotal() {
        return videoTotal;
    }

    public long getVideoUsedTotal() {
        return videoUsedTotal;
    }
}
