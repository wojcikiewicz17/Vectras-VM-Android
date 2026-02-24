package com.vectras.vm.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EndpointValidatorTest {

    @Test
    public void acceptsHttpsWithAllowedHost() {
        assertTrue(EndpointValidator.isAllowed("https://api.github.com/users/octocat"));
        assertTrue(EndpointValidator.isAllowed("https://go.anbui.ovh/egg/contentinfo?id=42"));
    }

    @Test
    public void rejectsHttpScheme() {
        assertFalse(EndpointValidator.isAllowed("http://api.github.com/users/octocat"));
    }

    @Test
    public void rejectsHostOutsideAllowlist() {
        assertFalse(EndpointValidator.isAllowed("https://example.com/users/octocat"));
    }

    @Test
    public void rejectsInvalidOrMalformedUrl() {
        assertFalse(EndpointValidator.isAllowed("not a url"));
        assertFalse(EndpointValidator.isAllowed("https:///missing-host"));
    }

    @Test
    public void rejectsForbiddenPortAndUserInfoPattern() {
        assertFalse(EndpointValidator.isAllowed("https://api.github.com:8443/users/octocat"));
        assertFalse(EndpointValidator.isAllowed("https://user@api.github.com/users/octocat"));
    }
}
