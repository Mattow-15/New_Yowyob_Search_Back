package com.yowyob.listing.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "La note est obligatoire")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 1000, message = "Le commentaire ne peut pas dépasser 1000 caractères")
    private String comment;

    @NotBlank(message = "L'identifiant utilisateur est obligatoire")
    private String userId;
}
