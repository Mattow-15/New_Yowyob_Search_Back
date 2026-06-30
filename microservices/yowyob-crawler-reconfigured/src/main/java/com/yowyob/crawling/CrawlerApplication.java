package com.yowyob.crawling;

import com.yowyob.crawling.infrastructure.config.CrawlerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CrawlerProperties.class)
public class CrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerApplication.class, args);
        System.out.println("═══════════════════════════════════════");
        System.out.println("   YowYob Crawling Service (Hexagonal)");
        System.out.println("   Démarré avec succès.");
        System.out.println("   Port   : 8086");
        System.out.println("═══════════════════════════════════════");
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public org.springframework.boot.CommandLineRunner testTrigger(com.yowyob.crawling.infrastructure.adapters.in.scheduler.CrawlerScheduler scheduler) {
        return args -> {
            if ("true".equalsIgnoreCase(System.getenv("SCRAPER_GOOGLELOCALMOCK_ENABLED"))
                    || "true".equalsIgnoreCase(System.getProperty("scraper.googlelocalmock.enabled"))) {
                System.out.println("Déclenchement immédiat du crawl E2E (Mock Google Local)");
                scheduler.runCrawl();
            }
        };
    }
}
