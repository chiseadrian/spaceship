package com.example.spaceship.application.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Component
public class ServletUriBuilder implements UriBuilder {

    @Override
    public URI buildUri(String path, Object... uriVariableValues) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(path)
                .buildAndExpand(uriVariableValues)
                .toUri();
    }
}