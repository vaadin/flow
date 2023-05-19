package com.vaadin.flow.theme;

import java.util.Arrays;
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

    @Test
    public void only_last_instance_of_base_url_should_be_replaced() {
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

        assertEquals("src/button/theme/custom/Button.html",
                theme.translateUrl("src/button/src/Button.html"));
    }

    private void assertUrlTranslations(AbstractTheme theme) {
        assertEquals("button/theme/custom/Button.html",
                theme.translateUrl("button/src/Button.html"));

        assertEquals("Non base url should return as was", "button/Button.html",
                theme.translateUrl("button/Button.html"));

        assertEquals("Non base url should return as was",
                "button/custom/Button.html",
                theme.translateUrl("button/custom/Button.html"));
    }

}
