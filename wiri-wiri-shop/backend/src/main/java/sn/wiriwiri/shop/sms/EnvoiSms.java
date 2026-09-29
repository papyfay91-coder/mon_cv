package sn.wiriwiri.shop.sms;

/** Passerelle SMS (Orange SMS API, Twilio, etc.). Une implémentation par fournisseur. */
public interface EnvoiSms {

    void envoyer(String telephone, String message);
}
