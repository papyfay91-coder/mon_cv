package sn.wiriwiri.shop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "wiriwiri.planification", name = "active", havingValue = "true", matchIfMissing = true)
public class PlanificationConfig {
}
