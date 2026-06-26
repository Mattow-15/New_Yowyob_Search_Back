package com.yowyob.search.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI / Swagger UI. UI : {@code /swagger-ui.html} — spec : {@code /v3/api-docs}.
 *
 * <p>Les trois en-têtes exigés par {@code /api/**} sont exposés comme schémas de sécurité
 * (« apiKey in header ») pour que le bouton <em>Authorize</em> de Swagger UI permette de les saisir :
 * <ul>
 *   <li>{@code X-Client-Id} + {@code X-Api-Key} : un clientApplication du kernel (auth déléguée).</li>
 *   <li>{@code X-Tenant-Id} : tenant de cloisonnement (cf. guide développeur).</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI yowyobSearchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("yowyob-search API")
                        .version("0.1.0")
                        .description("""
                                Service de recherche autonome multi-tenant.

                                Auth déléguée au kernel : chaque appel /api/** doit présenter
                                X-Client-Id + X-Api-Key d'un clientApplication du kernel (validés en
                                live via GET /api/client-applications/me), plus X-Tenant-Id.

                                X-Tenant-Id n'est PAS attribué par yowyob-search : c'est le tenantId
                                du kernel (UUID du tenant propriétaire des données). La même valeur
                                doit être utilisée à l'indexation et à la recherche."""))
                .components(new Components()
                        .addSecuritySchemes("X-Client-Id", headerKey("X-Client-Id",
                                "Identifiant du clientApplication kernel."))
                        .addSecuritySchemes("X-Api-Key", headerKey("X-Api-Key",
                                "Secret du clientApplication kernel."))
                        .addSecuritySchemes("X-Tenant-Id", headerKey("X-Tenant-Id",
                                "Tenant de cloisonnement (tenantId du kernel).")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("X-Client-Id").addList("X-Api-Key").addList("X-Tenant-Id"));
    }

    private static SecurityScheme headerKey(String headerName, String description) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(headerName)
                .description(description);
    }
}
