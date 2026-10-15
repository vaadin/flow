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
package com.vaadin.flow.quarkus.test;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Verifies that the build warns about a {@code quarkus.index-dependency}
 * reference to the deprecated {@code vaadin-jandex} artifact.
 */
public class DeprecatedJandexArtifactsWarningBuildTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.index-dependency.vaadin.group-id",
                    "com.vaadin")
            .overrideConfigKey("quarkus.index-dependency.vaadin.artifact-id",
                    "vaadin-jandex")
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= 900
                    && record.getMessage() != null
                    && record.getMessage()
                            .contains("deprecated Vaadin Jandex artifacts"))
            .assertLogRecords(records -> {
                Assertions.assertEquals(1, records.size());
                Assertions.assertTrue(records.get(0).getMessage().contains(
                        "the quarkus.index-dependency.vaadin configuration property"),
                        records.get(0).getMessage());
            });

    @Test
    public void indexDependencyReference_warningLogged() {
        // The log records are checked by assertLogRecords
    }
}
