package com.vectras.vm.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public enum EndpointFeature {
    LANGUAGE_MODULE_DOWNLOAD(
            new HashSet<>(Collections.singletonList("raw.githubusercontent.com")),
            Pattern.compile("^/rafaelmeloreisnovo/Vectras-VM-Android/main/resources/lang/.+\\.json$", Pattern.CASE_INSENSITIVE)
    ),
    GITHUB_USERS_API(
            new HashSet<>(Collections.singletonList("api.github.com")),
            Pattern.compile("^/users/.+", Pattern.CASE_INSENSITIVE)
    );

    private final Set<String> allowedHosts;
    private final Pattern allowedPathPattern;

    EndpointFeature(Set<String> allowedHosts, Pattern allowedPathPattern) {
        this.allowedHosts = Collections.unmodifiableSet(allowedHosts);
        this.allowedPathPattern = allowedPathPattern;
    }

    public boolean isAllowedHost(String normalizedHost) {
        return allowedHosts.contains(normalizedHost);
    }

    public boolean isAllowedPath(String path) {
        return allowedPathPattern == null || allowedPathPattern.matcher(path).matches();
    }

    public Set<String> getAllowedHosts() {
        return allowedHosts;
    }

    public String getAllowedPathPatternDescription() {
        return allowedPathPattern == null ? "<any>" : allowedPathPattern.pattern();
    }
}
