package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import org.threeten.bp.LocalDateTime;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Lifecell.ResultParserObserver;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType.Type;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.util.Async;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.util.Async.LoadAction;

public class Account implements Serializable {

    private final PhoneNumber phoneNumber;
    private final String superPassword;
    private final String token;
    private final String subscriptionId;

    public Account(PhoneNumber phoneNumber, String superPassword) {
        this(phoneNumber, superPassword, null, null);
    }

    public Account(PhoneNumber phoneNumber, String superPassword, String token, String subscriptionId) {
        this.phoneNumber = phoneNumber;
        this.superPassword = superPassword;
        this.token = token;
        this.subscriptionId = subscriptionId;
    }

    /*public static void main(String[] args) throws IOException {
        Account account = new Account("380936967829", "zueS4VN", "hPLOl6A");
        System.out.println(account.response("getSummaryData", "monthPeriod", "2017-10"));
    }*/

    public String response(String method, Object... paramsPart) throws IOException {
        Object[] params = new Object[8 + paramsPart.length];
        params[0] = "msisdn";
        params[1] = phoneNumber.toStringWithoutPlus();
        params[2] = "osType";
        params[3] = "ANDROID";
        params[4] = "languageId";
        params[5] = "ru";
        params[6] = "token";
        params[7] = token;
        for (int i = 0; i < paramsPart.length; i++) {
            params[i + 8] = paramsPart[i];
        }
        URL url = Lifecell.createUrl(method, params);
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        try(Reader in = new InputStreamReader(url.openStream(), "UTF-8")) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
            return out.toString();
        }
    }

    public Async<Balances> getBalancesAsync() {
        return new Async<>(new LoadAction<Balances>() {
            @Override
            public Balances onLoad() throws LifecellException, IOException {
                return getBalances();
            }
        });
    }

    public Balances getBalances() throws LifecellException, IOException {
        return observe("getBalances", new ResultParserObserver<Balances>() {
            @Override
            public Balances observeWithResult(XmlPullParser parser) throws LifecellException, XmlPullParserException, IOException {
                Balances.Builder builder = Balances.builder();
                builder.setPhoneNumber(getPhoneNumber());
                builder.setTime(LocalDateTime.now());

                boolean inResponse = false;

                int event = parser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    String name = parser.getName();
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            if(name.equalsIgnoreCase("response")) {
                                inResponse = true;
                            } else if(inResponse) {
                                if(name.equalsIgnoreCase("responseCode")) {
                                    try {
                                        ResponseType responseType = ResponseType
                                                .ofResponseCode(Long.parseLong(parser.nextText()));
                                        if(responseType.getType() == Type.ERROR) {
                                            throw new LifecellException(responseType);
                                        }
                                    } catch (NumberFormatException e) {
                                        throw new LifecellException(ResponseType.RESPONSE_STRUCTURE_CHANGED, e);
                                    }
                                } else if(isBalance(parser, "Bundle_Internet_LifeVideo")) {
                                    builder.setVideo((long) readBalance(parser));
                                } else if(isBalance(parser, "Bundle_Internet_3G_Tariff")) {
                                    builder.setInternet((long) readBalance(parser));
                                } else if(isBalance(parser, "Bundle_Voice_Offnet")) {
                                    builder.setOffnet((long) readBalance(parser));
                                } else if(isBalance(parser, "Line_Main")) {
                                    builder.setMain(readBalance(parser));
                                } else if(isBalance(parser, "Line_Bonus")) {
                                    builder.setBonus(readBalance(parser));
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if(name.equalsIgnoreCase("response")) {
                                inResponse = false;
                            }
                        default:
                            break;
                    }
                    event = parser.next();
                }
                if(!builder.hasAmounts()) {
                    throw new LifecellException(
                            "Video, internet, offnet, main or bonus not found in response.",
                            ResponseType.RESPONSE_STRUCTURE_CHANGED);
                }
                return builder.build();
            }
        });
    }

    private <T> T observe(String methodName, ResultParserObserver<T> observer) throws IOException, LifecellException {
        URL url = Lifecell.createUrl(methodName,
                "msisdn", phoneNumber.toStringWithoutPlus(),
                "languageId", "ru",
                "osType", "ANDROID",
                "token", token);
        return Lifecell.observeWithResult(url, observer);
    }

    private static boolean isBalance(XmlPullParser parser, String name) throws LifecellException, IOException, XmlPullParserException {
        if(parser.getName().equalsIgnoreCase("balance")) {
            String code = parser.getAttributeValue("", "code");
            if(name.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    private static double readBalance(XmlPullParser parser) throws LifecellException {
        try {
            return Double.parseDouble(parser.getAttributeValue("", "amount"));
        } catch (NumberFormatException e) {
            throw new LifecellException(ResponseType.RESPONSE_STRUCTURE_CHANGED, e);
        }
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public String getSuperPassword() {
        return superPassword;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getToken() {
        return token;
    }
}
