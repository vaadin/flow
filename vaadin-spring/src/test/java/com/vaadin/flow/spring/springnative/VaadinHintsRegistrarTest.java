/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
