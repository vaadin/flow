/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
