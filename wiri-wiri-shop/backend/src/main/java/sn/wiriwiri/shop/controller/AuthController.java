package sn.wiriwiri.shop.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.wiriwiri.shop.dto.DemandeOtp;
import sn.wiriwiri.shop.dto.JetonReponse;
import sn.wiriwiri.shop.dto.VerificationOtp;
import sn.wiriwiri.shop.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> demanderCode(@Valid @RequestBody DemandeOtp demande) {
        auth.demanderCode(demande.telephone());
        return Map.of("message", "Si ce numéro est valide, un code vous a été envoyé par SMS.");
    }

    @PostMapping("/verification")
    public JetonReponse verifier(@Valid @RequestBody VerificationOtp demande) {
        return auth.verifierCode(demande);
    }
}
