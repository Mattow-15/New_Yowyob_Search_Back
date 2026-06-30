package com.yowyob.search.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Réponse de la FAQ « Autres questions posées » générée dynamiquement (Groq). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaqResponse {

    private List<FaqItem> questions;
    /** true si généré par le LLM, false si repli statique (utile au debug/front). */
    private boolean generated;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FaqItem {
        private String question;
        private String answer;
    }
}
