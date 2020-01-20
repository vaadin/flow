/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.plugin.common;

import java.io.IOException;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ArtifactDataTest {
    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void constructor_nullFile() {
        expectedException.expect(NullPointerException.class);
        new ArtifactData(null, "one", "two");
    }

    @Test
    public void constructor_nullArtifactId() throws IOException {
        expectedException.expect(NullPointerException.class);
        new ArtifactData(testDirectory.newFile("test"), null, "two");
    }

    @Test
    public void constructor_nullVersion() throws IOException {
        expectedException.expect(NullPointerException.class);
        new ArtifactData(testDirectory.newFile("test"), "one", null);
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ArtifactData.class).verify();
    }
}
