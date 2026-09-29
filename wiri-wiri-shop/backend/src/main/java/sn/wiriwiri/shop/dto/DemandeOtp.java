package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemandeOtp(@NotBlank @Size(max = 20) String telephone) {
}
