package sn.wiriwiri.shop.ia;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/** Un client HTTP par service IA : ils peuvent pointer vers des fournisseurs différents. */
@Configuration
public class ClientIaConfig {

    @Bean
    public RestClient restClientTranscription(RestClient.Builder builder, WiriWiriProperties proprietes) {
        return client(builder, proprietes.ia().transcription(), proprietes.ia().delai());
    }

    @Bean
    public RestClient restClientExtraction(RestClient.Builder builder, WiriWiriProperties proprietes) {
        return client(builder, proprietes.ia().extraction(), proprietes.ia().delai());
    }

    private static RestClient client(RestClient.Builder builder, WiriWiriProperties.Service service, Duration delai) {
        var fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout((int) Math.min(delai.toMillis(), 10_000));
        fabrique.setReadTimeout((int) delai.toMillis());
        RestClient.Builder b = builder.clone()
                .baseUrl(service.baseUrl())
                .requestFactory(fabrique);
        if (service.aUneCle()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + service.cleApi());
        }
        return b.build();
    }
}
