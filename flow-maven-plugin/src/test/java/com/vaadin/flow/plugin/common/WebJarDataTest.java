/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.File;
import java.io.IOException;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vaadin Ltd.
 */
public class WebJarDataTest {
    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructor_nullFile() {
        new WebJarData(null, "one", "two");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_notExistingFile() {
        new WebJarData(new File("wow"), "one", "two");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_directoryInsteadOfFile() {
        new WebJarData(testDirectory.getRoot(), "one", "two");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullArtifactId() throws IOException {
        new WebJarData(testDirectory.newFile("test"), null, "two");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullVersion() throws IOException {
        new WebJarData(testDirectory.newFile("test"), "one", null);
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(WebJarData.class).verify();
    }
}
