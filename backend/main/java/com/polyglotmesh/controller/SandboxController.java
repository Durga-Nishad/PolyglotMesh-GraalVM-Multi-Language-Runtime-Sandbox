package com.polyglotmesh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.polyglotmesh.dto.SandboxPolicyResponse;
import com.polyglotmesh.dto.SelfTestResponse;
import com.polyglotmesh.sandbox.SandboxPolicyProvider;
import com.polyglotmesh.service.SandboxSelfTestService;
import com.polyglotmesh.util.Reactive;

import reactor.core.publisher.Mono;

/** Security page API: the active sandbox policy and the built-in attack self-test. */
@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    private final SandboxPolicyProvider policyProvider;
    private final SandboxSelfTestService selfTestService;

    public SandboxController(SandboxPolicyProvider policyProvider, SandboxSelfTestService selfTestService) {
        this.policyProvider = policyProvider;
        this.selfTestService = selfTestService;
    }

    @GetMapping("/policy")
    public Mono<SandboxPolicyResponse> policy() {
        return Mono.fromSupplier(policyProvider::describe);
    }

    @PostMapping("/self-test")
    public Mono<SelfTestResponse> selfTest() {
        return Reactive.blocking(selfTestService::run);
    }
}
