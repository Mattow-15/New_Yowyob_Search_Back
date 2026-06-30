package com.yowyob.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour l'ajout d'une entrée d'historique de recherche utilisateur.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryDto {
    private String query;
}
