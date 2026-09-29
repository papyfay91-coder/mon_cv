package sn.wiriwiri.shop.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HorlogeConfig {

    /** Toutes les dates sont stockées en UTC. */
    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
