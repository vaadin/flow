/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.PropertyData;

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

        Assert.assertEquals("onChangeHandler should have been set and value " +
                "updated", 5, myComponent.value);
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
                (int)data.getDefaultValue());

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
