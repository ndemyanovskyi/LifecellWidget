package ndemyanovskyi.lifecellwidget;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ndemyanovskyi.lifecellwidget.backend.InfoSmsReceiver;

public class Main {
    public static void main(String[] args) throws IOException {
        /*Document document = Jsoup.connect(
                //"https://auchan.ua/mobile-customer/account/login/?username=o.v.mekh@gmail.com&password=rinuza19&lang=ua&key=GeJtDzII017yM")
                "https://auchan.ua/api/barcode/4820047467231/store/20"
                //"https://auchan.ua/mobile-customer/account/info?email=o.v.mekh@gmail.com&frontend=1r7q4is5knbnqgdlr4n85efj97&lang=ua"

        )
                .ignoreContentType(true)
                .post();
        System.out.println(document.body());*/


        final Pattern PATTERN = Pattern.compile("\\+?([0-9]{3})([0-9]{2})([0-9]{7})");

        Matcher matcher = PATTERN.matcher("+380936967829");
        if(matcher.matches())
            System.out.println(matcher.group(1));
        System.out.println(matcher.group(2));
        System.out.println(matcher.group(3));
        /*String str = "Balans -50.00hrn, bonus +0.00hrn.\n" +
                "***\n" +
                "BEZLIM: dzvinky v lifecell, sots.merezhi. 8hv na inshi merezhi, 10GB 3G+, 9.8789898GB video. Taryf do kintsia doby. Nomer do 06.11.18.";
        InfoSmsReceiver.parseExpirationDate(null, str);*/
    }
}
