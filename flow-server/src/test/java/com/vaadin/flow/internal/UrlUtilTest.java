/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;

public class UrlUtilTest {
    private static final Set<String> FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES = new HashSet<>(
            Arrays.asList("http", "https", "mailto", "tel", "ftp"));

    private String encodeURIShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789;,/?:@&=+$-_.!~*'()#";
    private String encodeURIComponentShouldNotBeEscaped = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    @Test
    public void isExternal_URLStartsWithTwoSlashes_returnsTrue() {
        Assert.assertTrue(UrlUtil.isExternal("//foo"));
    }

    @Test
    public void isExternal_URLContainsAnySchemaAsPrefix_returnsTrue() {
        Assert.assertTrue(UrlUtil.isExternal("http://foo"));
        Assert.assertTrue(UrlUtil.isExternal("https://foo"));
        Assert.assertTrue(UrlUtil.isExternal("context://foo"));
        Assert.assertTrue(UrlUtil.isExternal("base://foo"));
    }

    @Test
    public void isExternal_URLDoesnotContainSchema_returnsFalse() {
        Assert.assertFalse(UrlUtil.isExternal("foo"));
    }

    @Test
    public void plusAndSpaceHandledCorrectly() {
        Assert.assertEquals("Plus+Spa%20+%20ce",
                UrlUtil.encodeURI("Plus+Spa + ce"));
        Assert.assertEquals("Plus%2BSpa%20%2B%20ce",
                UrlUtil.encodeURIComponent("Plus+Spa + ce"));
    }

    @Test
    public void encodeURI_shouldNotBeEscaped() {
        Assert.assertEquals(encodeURIShouldNotBeEscaped,
                UrlUtil.encodeURI(encodeURIShouldNotBeEscaped));
    }

    @Test
    public void encodeURI_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIShouldNotBeEscaped.contains(s)) {
                continue;
            }
            Assert.assertNotEquals(UrlUtil.encodeURI(s), s);
        }
    }

    @Test
    public void encodeURIComponent_shouldNotBeEscaped() {
        Assert.assertEquals(encodeURIComponentShouldNotBeEscaped, UrlUtil
                .encodeURIComponent(encodeURIComponentShouldNotBeEscaped));
    }

    @Test
    public void encodeURIComponent_mustBeEscaped() {
        for (char c = 0; c < 255; c++) {
            String s = String.valueOf(c);
            if (encodeURIComponentShouldNotBeEscaped.contains(s)) {
                continue;
            }
            Assert.assertNotEquals(UrlUtil.encodeURIComponent(s), s);
        }
    }

    @Test
    public void isSafeUrl_safeScheme_returnsTrue() {
        Assert.assertTrue(UrlUtil.isSafeUrl("https://vaadin.com",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_unsafeScheme_returnsFalse() {
        Assert.assertFalse(UrlUtil.isSafeUrl("javascript:alert(1)",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
        Assert.assertFalse(UrlUtil.isSafeUrl("data:text/html,<script>",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_schemeMatchIsCaseInsensitive_returnsFalse() {
        Assert.assertFalse(UrlUtil.isSafeUrl("JavaScript:alert(1)",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_relativeUrl_returnsTrue() {
        Assert.assertTrue(UrlUtil.isSafeUrl("/path/to/view",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
        Assert.assertTrue(
                UrlUtil.isSafeUrl("foo", FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_relativeUrlWithSpecialCharacters_returnsTrue() {
        // A strict URI parser would reject this, but it is a valid relative URL
        Assert.assertTrue(UrlUtil.isSafeUrl("/search?q=a b&x=[1]",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
        // A colon in the path must not be mistaken for a scheme separator
        Assert.assertTrue(UrlUtil.isSafeUrl("/path:with:colon",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_emptyOrBlank_returnsTrue() {
        Assert.assertTrue(
                UrlUtil.isSafeUrl("", FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
        Assert.assertTrue(
                UrlUtil.isSafeUrl("   ", FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_null_returnsFalse() {
        Assert.assertFalse(
                UrlUtil.isSafeUrl(null, FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_controlCharacterObfuscation_returnsFalse() {
        Assert.assertFalse(UrlUtil.isSafeUrl("java\tscript:alert(1)",
                FUTURE_25_2_DEFAULT_URL_SAFE_SCHEMES));
    }

    @Test
    public void isSafeUrl_wildcard_allowsAnyScheme() {
        Assert.assertTrue(UrlUtil.isSafeUrl("javascript:alert(1)",
                Collections.singleton(Constants.URL_SAFE_SCHEMES_WILDCARD)));
    }

    @Test
    public void isSafeUrl_configuredSchemes_replaceDefaults() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.getUrlSafeSchemes())
                .thenReturn(new HashSet<>(Arrays.asList("custom", "https")));
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);

        try (MockedStatic<VaadinService> mock = Mockito
                .mockStatic(VaadinService.class)) {
            mock.when(VaadinService::getCurrent).thenReturn(service);
            Assert.assertTrue(UrlUtil.isSafeUrl("custom:foo"));
            Assert.assertFalse(UrlUtil.isSafeUrl("mailto:a@b.com"));
            Assert.assertFalse(UrlUtil.isSafeUrl("javascript:alert(1)"));
        }
    }

    @Test
    public void isSafeUrl_configuredWildcard_allowsAnyScheme() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.getUrlSafeSchemes()).thenReturn(
                Collections.singleton(Constants.URL_SAFE_SCHEMES_WILDCARD));
        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);

        try (MockedStatic<VaadinService> mock = Mockito
                .mockStatic(VaadinService.class)) {
            mock.when(VaadinService::getCurrent).thenReturn(service);
            Assert.assertTrue(UrlUtil.isSafeUrl("javascript:alert(1)"));
        }
    }
}
