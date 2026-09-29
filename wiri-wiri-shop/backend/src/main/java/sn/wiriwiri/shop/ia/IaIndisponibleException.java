package sn.wiriwiri.shop.ia;

import org.springframework.http.HttpStatus;
import sn.wiriwiri.shop.exception.ApiException;

public class IaIndisponibleException extends ApiException {

    public IaIndisponibleException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "IA_INDISPONIBLE", message);
    }
}
