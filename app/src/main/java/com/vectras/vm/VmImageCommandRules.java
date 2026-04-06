package com.vectras.vm;

final class VmImageCommandRules {

    private VmImageCommandRules() {
    }

    static boolean isRawImageSizeTokenSafe(String sizeToken) {
        if (sizeToken == null) {
            return false;
        }
        String token = sizeToken.trim().toLowerCase();
        if (token.isEmpty()) {
            return false;
        }

        if (token.endsWith("t") || token.endsWith("p") || token.endsWith("e")) {
            return false;
        }
        if (token.endsWith("g")) {
            return token.length() <= 2;
        }
        if (token.endsWith("m")) {
            return token.length() <= 4;
        }
        if (token.endsWith("k")) {
            return token.length() <= 8;
        }
        return true;
    }
}
