package io.ldxinsight;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.config.EnableMongoAuditing; // 1. Import

@SpringBootApplication
@EnableMongoAuditing
public class LdxInsightBackendApplication {

    @Value("${server.port:8081}")
    private int serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public static void main(String[] args) {
        SpringApplication.run(LdxInsightBackendApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printSwaggerUrl() {
        String base = "http://localhost:" + serverPort + (contextPath == null ? "" : contextPath);
        System.out.println("\n==============================================");
        System.out.println("Swagger UI: " + base + "/swagger-ui/index.html");
        System.out.println("==============================================\n");
    }

}