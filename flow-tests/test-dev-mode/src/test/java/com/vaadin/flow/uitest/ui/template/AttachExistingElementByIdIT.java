/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Test;

public class AttachExistingElementByIdIT
        extends AbstractAttachExistingElementByIdIT {

    @Test
    public void elementsAreBoundOnTheServerSide() {
        open();

        assertTemplate("simple-path");
    }

    private void assertTemplate(String id) {
        assertTemplate(id, "default", "Type here to update label");
    }
}
