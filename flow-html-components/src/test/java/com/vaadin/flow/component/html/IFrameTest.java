/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class IFrameTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
        addOptionalStringProperty("srcdoc");
        addOptionalStringProperty("name");
        addOptionalStringProperty("allow");

        addProperty("importance", IFrame.ImportanceType.class,
                IFrame.ImportanceType.AUTO, IFrame.ImportanceType.HIGH, true,
                true);

        addProperty("sandbox", IFrame.SandboxType[].class, null,
                new IFrame.SandboxType[] { IFrame.SandboxType.ALLOW_POPUPS,
                        IFrame.SandboxType.ALLOW_MODALS },
                true, true);
    }

    @Test
    public void setSrc_unsafeScheme_throws() {
        IFrame iframe = new IFrame();
        Assert.assertThrows(IllegalArgumentException.class,
                () -> iframe.setSrc("javascript:alert(1)"));
    }

    @Test
    public void setUnsafeSrc_unsafeScheme_setsSrcWithoutValidation() {
        IFrame iframe = new IFrame();
        iframe.setUnsafeSrc("javascript:alert(1)");
        Assert.assertEquals("javascript:alert(1)", iframe.getSrc());
    }

    @Test
    public void constructor_unsafeSrc_throws() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new IFrame("javascript:alert(1)"));
    }
}
