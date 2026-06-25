/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.springnative;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class VaadinHintsRegistrarTest {

    @Test
    void shouldRegisterVaadinMetadata() {
        RuntimeHints hints = new RuntimeHints();
        new VaadinHintsRegistrar().registerHints(hints,
                getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("META-INF/VAADIN/conf/flow-build-info.json"))
                .accepts(hints);
    }

    @Test
    void shouldRegisterFeatureFlags() {
        RuntimeHints hints = new RuntimeHints();
        new VaadinHintsRegistrar().registerHints(hints,
                getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("vaadin-featureflags.properties")).accepts(hints);
    }

    @Test
    void shouldRegisterDefaultI18NPropertiesFiles() {
        RuntimeHints hints = new RuntimeHints();
        new VaadinHintsRegistrar().registerHints(hints,
                getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("vaadin-i18n/translations.properties"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("vaadin-i18n/translations_en.properties"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("vaadin-i18n/translations_it_IT.properties"))
                .accepts(hints);
    }

    @Test
    void shouldRegisterFlowServerStaticResources() {
        RuntimeHints hints = new RuntimeHints();
        new VaadinHintsRegistrar().registerHints(hints,
                getClass().getClassLoader());
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("com/vaadin/flow/router/NoRoutesError_dev.html"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("com/vaadin/flow/server/BootstrapHandler.js"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource()
                .forResource("com/vaadin/flow/server/frontend/index.ts"))
                .accepts(hints);
    }

}
