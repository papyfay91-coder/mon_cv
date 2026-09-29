package sn.wiriwiri.shop.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sn.wiriwiri.shop.service.StockageService;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StockageService stockage;

    public WebConfig(StockageService stockage) {
        this.stockage = stockage;
    }

    /** Images publiques des vitrines. Noms uniques (UUID) : cache long et immuable, idéal en 3G. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/fichiers/**")
                .addResourceLocations(stockage.racine().toUri().toString())
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
