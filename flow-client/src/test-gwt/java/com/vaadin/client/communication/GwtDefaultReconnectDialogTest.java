/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.client.communication;

import com.vaadin.client.ClientEngineTestBase;

public class GwtDefaultReconnectDialogTest extends ClientEngineTestBase {

    DefaultReconnectDialog dialog;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        dialog = new DefaultReconnectDialog();
    }

    public void testShow() {
        assertFalse(dialog.isVisible());
        dialog.show();
        assertTrue(dialog.isVisible());
        dialog.show();
        assertTrue(dialog.isVisible());
    }

    public void testHide() {
        dialog.show();
        dialog.hide();
        assertFalse(dialog.isVisible());
        dialog.hide();
        assertFalse(dialog.isVisible());
    }
}
