package sn.wiriwiri.shop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import sn.wiriwiri.shop.sms.EnvoiSms;

/** Remplace la passerelle SMS pendant les tests et mémorise le dernier code reçu par numéro. */
@TestConfiguration
public class SmsCapture {

    public static final Map<String, String> DERNIER_CODE = new ConcurrentHashMap<>();
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Bean
    EnvoiSms envoiSmsTest() {
        return (telephone, message) -> {
            Matcher m = CODE.matcher(message);
            if (m.find()) {
                DERNIER_CODE.put(telephone, m.group(1));
            }
        };
    }
}
