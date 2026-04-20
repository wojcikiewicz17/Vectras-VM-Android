package com.vectras.vm.setupwizard;

final class BootstrapUrlNormalizer {
    private BootstrapUrlNormalizer() {
    }

    static String normalizePath(String encodedPath) {
        if (encodedPath == null || encodedPath.isEmpty()) {
            return "/";
        }

        String path = encodedPath.trim();
        if (path.contains("://") || path.startsWith("\\")) {
            return "/";
        }
        if (path.startsWith("//")) {
            int nextSlash = path.indexOf('/', 2);
            String firstSegment = nextSlash == -1 ? path.substring(2) : path.substring(2, nextSlash);
            if (firstSegment.contains(".") || firstSegment.contains(":")) {
                return "/";
            }
        }
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }
}
