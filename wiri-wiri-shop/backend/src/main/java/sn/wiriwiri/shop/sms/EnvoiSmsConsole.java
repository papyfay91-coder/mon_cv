package sn.wiriwiri.shop.sms;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import sn.wiriwiri.shop.util.Telephones;

/**
 * Implémentation de DÉVELOPPEMENT UNIQUEMENT : écrit le SMS dans les journaux.
 * En production, fournir un bean EnvoiSms branché sur un opérateur et définir
 * wiriwiri.sms.fournisseur avec une autre valeur que "console".
 */
@Component
@ConditionalOnProperty(prefix = "wiriwiri.sms", name = "fournisseur", havingValue = "console", matchIfMissing = true)
public class EnvoiSmsConsole implements EnvoiSms {

    private static final Logger LOG = LoggerFactory.getLogger(EnvoiSmsConsole.class);

    @PostConstruct
    void avertir() {
        LOG.warn("Envoi de SMS en mode CONSOLE : les codes OTP apparaissent dans les journaux. Ne pas utiliser en production.");
    }

    @Override
    public void envoyer(String telephone, String message) {
        LOG.info("[SMS -> {}] {}", Telephones.masquer(telephone), message);
    }
}
