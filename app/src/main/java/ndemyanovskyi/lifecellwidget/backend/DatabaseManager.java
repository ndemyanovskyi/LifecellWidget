package ndemyanovskyi.lifecellwidget.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Objects;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Balances;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Balances.Builder;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.ExpirationDate;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.PhoneNumber;

public class DatabaseManager extends SQLiteOpenHelper {

    private static final int VERSION = 1;
    private static final String NAME = "balances";

    public static final String BALANCES_TABLE_NAME = "balances";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_NAME_TIME = "time";
    public static final String COLUMN_NAME_MAIN = "main";
    public static final String COLUMN_NAME_BONUS = "bonus";
    public static final String COLUMN_NAME_OFFNET = "offnet";
    public static final String COLUMN_NAME_INTERNET = "internet";
    public static final String COLUMN_NAME_VIDEO = "video";

    public static final String EXPIRATION_DATES_TABLE_NAME = "expiration_dates";
    public static final String COLUMN_NAME_VALUE = "value";

    private DatabaseManager(Context context) {
        super(context, NAME, null, VERSION);
    }

    public static DatabaseManager from(Context context) {
        return new DatabaseManager(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + BALANCES_TABLE_NAME + " (" +
                COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_PHONE_NUMBER + " INTEGER NOT NULL, " +
                COLUMN_NAME_TIME + " TEXT NOT NULL UNIQUE, " +
                COLUMN_NAME_MAIN + " REAL NOT NULL, " +
                COLUMN_NAME_BONUS + " REAL NOT NULL, " +
                COLUMN_NAME_OFFNET + " INTEGER NOT NULL, " +
                COLUMN_NAME_INTERNET + " INTEGER NOT NULL, " +
                COLUMN_NAME_VIDEO + " INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE " + EXPIRATION_DATES_TABLE_NAME + " (" +
                COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_PHONE_NUMBER + " INTEGER NOT NULL, " +
                COLUMN_NAME_TIME + " TEXT NOT NULL UNIQUE, " +
                COLUMN_NAME_VALUE + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Database is not upgradable.");
    }

    public static Balances readNewestBalances(Context context, PhoneNumber phoneNumber) {
        return from(context).readNewestBalances(phoneNumber);
    }

    public void insertBalances(Balances balances) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_PHONE_NUMBER, balances.getPhoneNumber().toLong());
        values.put(COLUMN_NAME_BONUS, balances.getBonus());
        values.put(COLUMN_NAME_MAIN, balances.getMain());
        values.put(COLUMN_NAME_TIME, Objects.toString(balances.getTime()));
        values.put(COLUMN_NAME_INTERNET, balances.getInternet());
        values.put(COLUMN_NAME_VIDEO, balances.getVideo());
        values.put(COLUMN_NAME_OFFNET, balances.getOffnet());
        getWritableDatabase().insertOrThrow(BALANCES_TABLE_NAME, null, values);
    }

    public void insertExpirationDate(ExpirationDate expirationDate) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_PHONE_NUMBER, expirationDate.getPhoneNumber().toLong());
        values.put(COLUMN_NAME_TIME, expirationDate.getTime().toString());
        values.put(COLUMN_NAME_VALUE, expirationDate.getValue().toString());
        getWritableDatabase().insertOrThrow(EXPIRATION_DATES_TABLE_NAME, null, values);
    }

    public Balances readNewestBalances(PhoneNumber phoneNumber) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readBalances(database, phoneNumber, null, "TIME DESC");
        }
    }

    public Balances readFirstBalancesToday(PhoneNumber phoneNumber) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readBalances(database, phoneNumber, "TIME >= DATE('NOW')", "TIME ASC");
        }
    }

    private static Balances readBalances(SQLiteDatabase database, PhoneNumber phoneNumber, String where, String orderBy) {
        String query = String.format(
                "SELECT * FROM %s WHERE PHONE_NUMBER=%s %s %s LIMIT 1",
                BALANCES_TABLE_NAME, phoneNumber.toLong(),
                where != null ? "AND " + where : "",
                orderBy != null ? "ORDER BY " + orderBy : "");
        try(Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToNext()) {
                Builder builder = Balances.builder();
                builder.setPhoneNumber(PhoneNumber.of(cursor.getLong(1)));
                builder.setTime(LocalDateTime.parse(cursor.getString(2)));
                builder.setMain(cursor.getDouble(3));
                builder.setBonus(cursor.getDouble(4));
                builder.setOffnet(cursor.getLong(5));
                builder.setInternet(cursor.getLong(6));
                builder.setVideo(cursor.getLong(7));
                builder.setExpirationDate(readExpirationDate(database, phoneNumber, builder.getTime()));
                return builder.build();
            }
            return null;
        }
    }

    public LocalDateTime readBalancesUpdateTime(PhoneNumber phoneNumber) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readUpdateTime(database, phoneNumber, BALANCES_TABLE_NAME);
        }
    }

    public LocalDateTime readExpirationDateUpdateTime(PhoneNumber phoneNumber) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readUpdateTime(database, phoneNumber, EXPIRATION_DATES_TABLE_NAME);
        }
    }

    public ExpirationDate readNewestExpirationDate(PhoneNumber phoneNumber) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readExpirationDate(database, phoneNumber, null, "TIME DESC");
        }
    }

    public ExpirationDate readExpirationDate(PhoneNumber phoneNumber, LocalDateTime time) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return readExpirationDate(database, phoneNumber, time);
        }
    }

    private static ExpirationDate readExpirationDate(SQLiteDatabase database, PhoneNumber phoneNumber, LocalDateTime time) {
        ExpirationDate beforeExpirationDate = readBeforeExpirationDate(database, phoneNumber, time);
        ExpirationDate afterExpirationDate = readAfterExpirationDate(database, phoneNumber, time);
        if (beforeExpirationDate != null) {
            if (afterExpirationDate != null) {
                long beforeMillis = beforeExpirationDate.getTime().until(time, ChronoUnit.MILLIS);
                long afterMillis = afterExpirationDate.getTime().until(time, ChronoUnit.MILLIS);
                return (beforeMillis < afterMillis) ? beforeExpirationDate : afterExpirationDate;
            } else {
                return beforeExpirationDate;
            }
        } else {
            return afterExpirationDate;
        }
    }

    private static ExpirationDate readBeforeExpirationDate(SQLiteDatabase database, PhoneNumber phoneNumber, LocalDateTime time) {
        return readExpirationDate(database, phoneNumber, "TIME <= '" + time + "'", "TIME DESC");
    }

    private static ExpirationDate readAfterExpirationDate(SQLiteDatabase database, PhoneNumber phoneNumber, LocalDateTime time) {
        return readExpirationDate(database, phoneNumber, "TIME >= '" + time + "'", "TIME ASC");
    }

    private static ExpirationDate readExpirationDate(SQLiteDatabase db, PhoneNumber phoneNumber, String where, String orderBy) {
        String query = String.format(
                "SELECT * FROM %s WHERE PHONE_NUMBER=%s %s %s LIMIT 1",
                EXPIRATION_DATES_TABLE_NAME, phoneNumber.toLong(),
                where != null ? "AND " + where : "",
                orderBy != null ? "ORDER BY " + orderBy : "");
        try(Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToNext()) {
                return new ExpirationDate(
                        PhoneNumber.of(cursor.getLong(1)),
                        LocalDateTime.parse(cursor.getString(2)),
                        LocalDate.parse(cursor.getString(3)));
            }
            return null;
        }
    }

    private static LocalDateTime readUpdateTime(SQLiteDatabase database, PhoneNumber phoneNumber, String tableName) {
        String query = String.format(
                "SELECT TIME FROM %s WHERE PHONE_NUMBER=%s ORDER BY TIME DESC LIMIT 1",
                tableName, phoneNumber.toLong());
        try(Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToNext()) {
                return LocalDateTime.parse(cursor.getString(0));
            }
            return null;
        }
    }
}
