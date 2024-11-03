package com.example.spaceship.application.util;

import java.net.URI;

public interface UriBuilder {
    URI buildUri(String path, Object... uriVariableValues);
}