/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

public class PropertyConfigurationImplTest {

    PropertyConfigurationImpl<MyComponent, Integer> intPropertyConf;

    @Before
    public void init() {
        intPropertyConf = new PropertyConfigurationImpl<>(MyComponent.class,
                "int", Integer.class, 1);
    }

    @Test
    public void onChange() {
        intPropertyConf.onChange(MyComponent::setInt);

        MyComponent myComponent = new MyComponent();

        intPropertyConf.getOnChangeHandler().accept(myComponent, 5);

        Assert.assertEquals(
                "onChangeHandler should have been set and value " + "updated",
                5, myComponent.value);
    }

    @Test(expected = IllegalStateException.class)
    public void onChange_throwsIfCalledTwice() {
        intPropertyConf.onChange(MyComponent::setInt);
        intPropertyConf.onChange(MyComponent::setInt);
    }

    @Test
    public void readOnly() {
        intPropertyConf.readOnly();

        PropertyData<Integer> data = intPropertyConf.getPropertyData();

        // verify default value for completeness
        Assert.assertEquals("default value is 1", 1,
                (int) data.getDefaultValue());

        Assert.assertTrue("read-only flag should have been set to true",
                data.isReadOnly());
    }

    @Tag("for-reasons")
    private static final class MyComponent extends Component {
        private int value;

        public void setInt(int newValue) {
            this.value = newValue;
        }
    }
}
