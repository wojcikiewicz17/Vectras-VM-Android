package com.vectras.vm.network;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;

public final class EndpointValidator {
    private EndpointValidator() {
    }

    public static void validateOrThrow(String url, EndpointFeature feature) {
        if (feature == null) {
            throw new IllegalArgumentException("Endpoint feature is required");
        }

        final URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid endpoint URL", e);
        }

        final String scheme = uri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only HTTPS endpoints are allowed");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("User info is not allowed in endpoint URLs");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint host is required");
        }

        final String normalizedHost;
        try {
            normalizedHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES)
                    .toLowerCase(Locale.US)
                    .replaceAll("\\.$", "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid endpoint host", e);
        }

        if (!feature.isAllowedHost(normalizedHost)) {
            throw new IllegalArgumentException("Host is not allowlisted for feature: " + normalizedHost);
        }

        final int port = uri.getPort();
        if (port != -1 && port != 443) {
            throw new IllegalArgumentException("Unexpected endpoint port: " + port);
        }

        final String path = uri.getPath() == null ? "" : uri.getPath();
        if (!feature.isAllowedPath(path)) {
            throw new IllegalArgumentException(
                    "Path is not allowed for feature. Expected pattern: " + feature.getAllowedPathPatternDescription());
        }
    }

    public static boolean isAllowed(String url, EndpointFeature feature) {
        try {
            validateOrThrow(url, feature);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
