package sn.wiriwiri.shop.ia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import sn.wiriwiri.shop.config.WiriWiriProperties;

@Configuration
public class ClientIaConfig {

    @Bean
    public RestClient restClientIa(RestClient.Builder builder, WiriWiriProperties proprietes) {
        var ia = proprietes.ia();
        var fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout((int) Math.min(ia.delai().toMillis(), 10_000));
        fabrique.setReadTimeout((int) ia.delai().toMillis());
        RestClient.Builder b = builder.clone()
                .baseUrl(ia.baseUrl())
                .requestFactory(fabrique);
        if (ia.cleApi() != null && !ia.cleApi().isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ia.cleApi());
        }
        return b.build();
    }
}
