package com.raulb.db_unify_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapInitializer implements ApplicationRunner {
    private final ConnectionService service;

    @Override
    public void run(ApplicationArguments args) {
        service.initializeAllConnections();
    }
}