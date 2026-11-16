package com.example.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Makes one RestClient available to the whole application.
 *
 * RestClient is how a Spring service calls another service over HTTP.
 * The addresses it calls are not here: they come from application.properties
 * so docker-compose can override them with environment variables.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
