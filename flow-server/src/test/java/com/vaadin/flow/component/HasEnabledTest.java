/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

public class HasEnabledTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasComponents {

    }

    @Test
    public void enabledComponent_isEnabledReturnsTrue() {
        TestComponent component = new TestComponent();

        Assert.assertTrue(component.isEnabled());
    }

    @Test
    public void explicitlyDisabledComponent_isEnabledReturnsFalse() {
        TestComponent component = new TestComponent();
        component.setEnabled(false);

        Assert.assertFalse(component.isEnabled());
    }

    @Test
    public void implicitlyDisabledComponent_isEnabledReturnsFalse() {
        TestComponent component = new TestComponent();

        TestComponent parent = new TestComponent();
        parent.setEnabled(false);

        parent.add(component);

        Assert.assertFalse(component.isEnabled());
    }

    @Test
    public void implicitlyDisabledComponent_detach_componentBecomesEnabled() {
        TestComponent component = new TestComponent();

        TestComponent parent = new TestComponent();
        parent.add(component);

        parent.setEnabled(false);

        parent.remove(component);

        Assert.assertTrue(component.isEnabled());
    }

    @Test
    public void explicitlyDisabledComponent_enableParent_componentRemainsDisabled() {
        TestComponent component = new TestComponent();
        component.setEnabled(false);

        TestComponent parent = new TestComponent();
        parent.add(component);

        parent.setEnabled(false);

        Assert.assertFalse(component.isEnabled());

        parent.setEnabled(true);

        Assert.assertFalse(component.isEnabled());
    }

}
