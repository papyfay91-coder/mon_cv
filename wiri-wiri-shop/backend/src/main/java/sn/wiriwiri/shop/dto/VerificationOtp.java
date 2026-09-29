package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** nomVendeur n'est requis qu'à la première connexion (création de la boutique). */
public record VerificationOtp(
        @NotBlank @Size(max = 20) String telephone,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres") String code,
        @Size(max = 100) String nomVendeur) {
}
