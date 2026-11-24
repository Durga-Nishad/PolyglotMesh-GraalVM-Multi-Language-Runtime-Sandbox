package com.polyglotmesh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.polyglotmesh.dto.ExecutionRequest;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.sandbox.SupportedLanguages;
import com.polyglotmesh.service.ExecutionService;
import com.polyglotmesh.util.Reactive;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/** Execution API: run code and list the supported languages. */
@RestController
@RequestMapping("/api/execution")
public class ExecutionController {

    private final ExecutionService executionService;
    private final SupportedLanguages languages;

    public ExecutionController(ExecutionService executionService, SupportedLanguages languages) {
        this.executionService = executionService;
        this.languages = languages;
    }

    @PostMapping("/run")
    public Mono<ExecutionResponse> run(@Valid @RequestBody ExecutionRequest request) {
        return Reactive.blocking(() -> executionService.run(request));
    }

    @GetMapping("/languages")
    public Mono<List<String>> supportedLanguages() {
        return Mono.fromSupplier(() -> List.copyOf(languages.executableLanguages()));
    }
}
