/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.UsageStatistics.UsageEntry;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlComponentUsageStatisticsTest {

    static Stream<Arguments> trackedComponents() {
        return Stream.of(
                Arguments.of(
                        Named.<Supplier<Component>> of("Audio", Audio::new),
                        "flow/Audio"),
                Arguments.of(
                        Named.<Supplier<Component>> of("Video", Video::new),
                        "flow/Video"),
                Arguments.of(
                        Named.<Supplier<Component>> of("Table", Table::new),
                        "flow/Table"),
                Arguments.of(Named.<Supplier<Component>> of("NativeForm",
                        NativeForm::new), "flow/NativeForm"));
    }

    @ParameterizedTest
    @MethodSource("trackedComponents")
    void createComponent_usageIsReported(Supplier<Component> factory,
            String entryName) {
        factory.get();

        assertTrue(
                UsageStatistics.getEntries().map(UsageEntry::getName)
                        .anyMatch(entryName::equals),
                "Expected usage statistics entry " + entryName);
    }
}
