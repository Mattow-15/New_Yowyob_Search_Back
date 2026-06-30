package com.yowyob.crawling.infrastructure.adapters.in.mock;

import com.yowyob.crawling.infrastructure.adapters.out.kernel.dto.KernelAgencyListResponse;
import com.yowyob.crawling.infrastructure.adapters.out.kernel.dto.KernelAgencyResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Faux organization-core, actif UNIQUEMENT sous le profil "mock".
 * Reproduit le VRAI contrat : GET /api/organizations/{organizationId}/agencies
 * renvoyant l'enveloppe { success, data:[AgencyResponse] }, sans pagination.
 * Permet de prouver toute la chaîne Voie 1 sans Kernel réel :
 *   ce contrôleur -> OrganizationDirectoryAdapter -> KernelOrgSyncService
 *   -> ListingKafkaProducer (source=KERNEL_ORG) -> listing-service -> RabbitMQ -> search -> ES.
 *
 * Renvoie les types réels : si le DTO évolue, ce stub casse à la COMPILATION, pas au runtime.
 */
@RestController
@Profile("mock")
public class KernelOrgMockController {

    @GetMapping("/api/organizations/{organizationId}/agencies")
    public KernelAgencyListResponse agencies(@PathVariable String organizationId) {
        List<KernelAgencyResponse> data = List.of(
            agency("agc-001", organizationId, "Pharmacie du Centre", "PHARMACY",
                   3.8662, 11.5174, "Avenue Kennedy", "Yaoundé",
                   "+237 690000001", "contact@pharmaciecentre.cm", "08:00", "20:00"),
            agency("agc-002", organizationId, "Boulangerie Saker", "BAKERY",
                   3.8480, 11.5021, "Rue Foch", "Yaoundé",
                   "+237 690000002", "bonjour@saker.cm", "06:00", "21:00"),
            agency("agc-003", organizationId, "Quincaillerie Mvog-Mbi", "HARDWARE",
                   3.8550, 11.5280, "Carrefour Mvog-Mbi", "Yaoundé",
                   "+237 690000003", "ventes@quincaillerie-mvogmbi.cm", "07:30", "18:00")
        );
        return new KernelAgencyListResponse(true, data, "OK", null, "2026-06-13T09:00:00Z");
    }

    private KernelAgencyResponse agency(String id, String organizationId, String name,
                                        String agencyType, double lat, double lng,
                                        String location, String city, String phone,
                                        String email, String openTime, String closeTime) {
        return new KernelAgencyResponse(
                id, organizationId, name, name, name, agencyType, null,
                location, city, "Cameroun", lat, lng, openTime, closeTime,
                phone, email, List.of(), null,
                // Gouvernance : agence active, publique, non supprimée → indexable.
                true, true, false, null);
    }
}
