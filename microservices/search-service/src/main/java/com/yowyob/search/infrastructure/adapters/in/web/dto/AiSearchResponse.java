package com.yowyob.search.infrastructure.adapters.in.web.dto;

import com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchResponse {

    // Réponse générée par le LLM
    private String aiAnswer;

    // Intention détectée
    private String intent;

    // Requête reformulée
    private String rewrittenQuery;

    // Documents Elasticsearch utilisés comme contexte
    private List<ProductDocument> sources;

    // Mode IA (fan-out) : intitulés des sous-recherches lancées en parallèle.
    // Null pour l'Aperçu IA mono-recherche. Permet d'afficher "j'ai cherché : X · Y · Z".
    private List<String> subQueries;

    // Temps de traitement
    private long processingTimeMs;

    // Indique si la réponse vient du mode IA ou du mode classique
    private boolean aiMode;
}
