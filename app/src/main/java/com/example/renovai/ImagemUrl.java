package com.example.renovai;

import java.net.URI;

/** A chave do cache é a URL completa, incluindo a versão do asset. */
public final class ImagemUrl {
    private ImagemUrl() {}

    public static String normalizar(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null) return null;
            return uri.toASCIIString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
