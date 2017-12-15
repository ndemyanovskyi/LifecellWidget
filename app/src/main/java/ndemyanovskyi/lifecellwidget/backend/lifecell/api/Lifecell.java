package ndemyanovskyi.lifecellwidget.backend.lifecell.api;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType.Type;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.util.HmacSha1Signature;

public class Lifecell {

    private static final String SECRET_KEY = "E6j_$4UnR_)0b";
    private static final int ACCESS_KEY = 7;

    private static final String BASE_URL = "https://api.life.com.ua/mobile/";

    public static Account signIn(Account account) throws LifecellException {
        return signIn(account.getPhoneNumber(), account.getSuperPassword());
    }

    public static Account signIn(String phoneNumber, String superPassword) throws LifecellException {
        return signIn(PhoneNumber.parse(phoneNumber), superPassword);
    }

    public static Account signIn(final PhoneNumber phoneNumber, final String superPassword) throws LifecellException {
        URL url = createUrl("signIn", "msisdn", phoneNumber.toStringWithoutPlus(), "superPassword", superPassword);
        return observeWithResult(url, new ResultParserObserver<Account>() {
            @Override
            public Account observeWithResult(XmlPullParser parser) throws LifecellException, XmlPullParserException, IOException {
                boolean inResponse = false;
                String token = null;
                String subId = null;

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
                                } else if(name.equalsIgnoreCase("token")) {
                                    token = parser.nextText();
                                } else if(name.equalsIgnoreCase("subId")) {
                                    subId = parser.nextText();
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
                if(token == null || subId == null) {
                    throw new LifecellException(
                            "Token or SubId not found in response.",
                            ResponseType.RESPONSE_STRUCTURE_CHANGED);
                }
                return new Account(phoneNumber, superPassword, token, subId);
            }
        });
    }

    static <T> T observeWithResult(URL url, ResultParserObserver<T> observer) throws LifecellException {
        XmlPullParser parser;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
        } catch (XmlPullParserException e) {
            throw new Error("Illegal program state. XmlPullParser creation must be always work.", e);
        }

        try(InputStream inputStream = url.openStream()) {
            parser.setInput(inputStream, "UTF-8");
            return observer.observeWithResult(parser);
        } catch (XmlPullParserException e) {
            throw new LifecellException(ResponseType.RESPONSE_STRUCTURE_CHANGED, e);
        } catch (IOException e) {
            throw new LifecellException(ResponseType.IO_ERROR, e);
        }
    }

    static void observe(URL url, ParserObserver parserObserver) throws LifecellException {
        XmlPullParser parser;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
        } catch (XmlPullParserException e) {
            throw new Error("Illegal program state. XmlPullParser creation must be always work.", e);
        }

        try(InputStream inputStream = url.openStream()) {
            parser.setInput(inputStream, "UTF-8");
            parserObserver.observe(parser);
        } catch (XmlPullParserException e) {
            throw new LifecellException(ResponseType.RESPONSE_STRUCTURE_CHANGED, e);
        } catch (IOException e) {
            throw new LifecellException(ResponseType.IO_ERROR, e);
        }
    }

    static URL createUrl(String methodName, Object... parameters) {
        if(parameters.length % 2 != 0) {
            throw new IllegalArgumentException("Parameters must be paired.");
        }

        StringBuilder builder = new StringBuilder();
        builder.append(methodName).append("?accessKeyCode=").append(ACCESS_KEY);

        for(int i = 0; i < parameters.length; i += 2) {
            builder.append("&");
            builder.append(parameters[i]);
            builder.append("=");
            builder.append(parameters[i + 1]);
        }
        builder.append("&signature=");
        String signature;
        try {
            signature = HmacSha1Signature.calculateRFC2104HMAC(builder.toString(), SECRET_KEY);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new Error("Illegal program state. Signature generation must be always work.", e);
        }
        builder.append(signature);
        try {
            return new URL(BASE_URL + builder.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    interface ResultParserObserver<T> {
        T observeWithResult(XmlPullParser parser) throws LifecellException, XmlPullParserException, IOException;
    }

    interface ParserObserver {
        void observe(XmlPullParser parser) throws LifecellException, XmlPullParserException, IOException;
    }

}
