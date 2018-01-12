package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractThemeTest {
    Supplier<Stream<String>> defaultResources = () -> Arrays
            .asList("/webjars/button/2.0.0/src",
                    "/webjars/button/2.0.0/src/Button.html",
                    "/webjars/button/2.0.0/theme/",
                    "/webjars/button/2.0.0/theme/custom/",
                    "/webjars/button/2.0.0/theme/custom/Button.html",
                    "/webjars/button/2.0.0/Button.html")
            .stream();

    @Test
    public void default_url_translation_returns_correct_result() {
        AbstractTheme theme = new AbstractTheme() {
            @Override
            public String getBaseUrl() {
                return "src/";
            }

            @Override
            public String getThemeUrl() {
                return "theme/custom/";
            }
        };

        assertUrlTranslations(theme);
    }

    @Test
    public void default_url_translation_returns_given_url_if_translated_not_in_resources() {
        AbstractTheme theme = new AbstractTheme() {
            @Override
            public String getBaseUrl() {
                return "src/";
            }

            @Override
            public String getThemeUrl() {
                return "theme/custom/";
            }
        };

        assertEquals("Got wrong url when expected untranslated to be returned",
                "button/src/Button-mixin.html",
                theme.getTranslatedUrl("button/src/Button-mixin.html",
                        defaultResources.get()));
    }

    @Test
    public void default_url_translation_returns_correct_result_for_wrong_end_in_base_url() {
        AbstractTheme theme = new AbstractTheme() {
            @Override
            public String getBaseUrl() {
                return "src";
            }

            @Override
            public String getThemeUrl() {
                return "theme/custom/";
            }
        };

        assertUrlTranslations(theme);
    }

    @Test
    public void default_url_translation_returns_correct_result_for_different_end_in_theme_url() {
        AbstractTheme theme = new AbstractTheme() {
            @Override
            public String getBaseUrl() {
                return "src/";
            }

            @Override
            public String getThemeUrl() {
                return "theme/custom";
            }
        };

        assertUrlTranslations(theme);
    }

    private void assertUrlTranslations(AbstractTheme theme) {
        assertEquals("button/theme/custom/Button.html", theme.getTranslatedUrl(
                "button/src/Button.html", defaultResources.get()));

        assertEquals("Non base url should return as was", "button/Button.html",
                theme.getTranslatedUrl("button/Button.html",
                        defaultResources.get()));

        assertEquals("Non base url should return as was",
                "button/custom/Button.html", theme.getTranslatedUrl(
                        "button/custom/Button.html", defaultResources.get()));
    }

}
