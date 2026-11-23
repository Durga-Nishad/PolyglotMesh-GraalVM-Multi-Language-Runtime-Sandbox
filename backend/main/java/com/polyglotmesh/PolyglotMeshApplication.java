package com.polyglotmesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the PolyglotMesh backend.
 * Run with: mvn spring-boot:run   (from the backend/ folder)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PolyglotMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolyglotMeshApplication.class, args);
    }
}
