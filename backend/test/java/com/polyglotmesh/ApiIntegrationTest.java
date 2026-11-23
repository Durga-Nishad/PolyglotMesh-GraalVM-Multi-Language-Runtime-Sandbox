package com.polyglotmesh;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/** End-to-end API checks (no database in Week 1-2). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "120000")
class ApiIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private com.polyglotmesh.engine.PolyglotEngineService engine;

    private static boolean warmedUp = false;

    /** The first Python/JS start is slow on a normal JDK, so start both once before the tests. */
    @org.junit.jupiter.api.BeforeEach
    void warmUp() {
        if (!warmedUp) {
            engine.execute(com.polyglotmesh.engine.ExecutionCommand.of("javascript", "1 + 1"));
            engine.execute(com.polyglotmesh.engine.ExecutionCommand.of("python", "print(1)"));
            warmedUp = true;
        }
    }

    @Test
    void runEndpointExecutesJavaScript() {
        client.post().uri("/api/execution/run").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("language", "javascript", "code", "console.log('hi from js')"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.output").value(out -> org.junit.jupiter.api.Assertions.assertTrue(out.toString().contains("hi from js")));
    }

    @Test
    void runEndpointRejectsBlankCode() {
        client.post().uri("/api/execution/run").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("language", "python", "code", "   "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.code").exists();
    }

    @Test
    void runEndpointRejectsUnsupportedLanguage() {
        client.post().uri("/api/execution/run").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("language", "cobol", "code", "DISPLAY 'HI'"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors.language").exists();
    }

    @Test
    void runEndpointRejectsMissingBody() {
        client.post().uri("/api/execution/run").contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void blockedCodeIsReportedNotThrown() {
        client.post().uri("/api/execution/run").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("language", "python", "code", "import os\nprint(os.listdir('/'))"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo("BLOCKED");
    }
}
