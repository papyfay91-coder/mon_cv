package sn.wiriwiri.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WiriWiriShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(WiriWiriShopApplication.class, args);
    }
}
