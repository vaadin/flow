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
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

public class HasHelperTest {

    @Tag("div")
    public static class HasHelperComponent extends Component implements HasHelper {
    }

    @Test
    public void setHelperText() {
        final HasHelperComponent c = new HasHelperComponent();
        c.setHelperText("helper");
        Assert.assertEquals("helper", c.getHelperText());
    }

    @Test
    public void setHelperComponent() {
        final HasHelperComponent c = new HasHelperComponent();
        final HasHelperComponent slotted = new HasHelperComponent();
        c.setHelperComponent(slotted);
        Assert.assertEquals(slotted, c.getHelperComponent());
    }

    @Test
    public void removeHelperText() {
        final HasHelperComponent c = new HasHelperComponent();
        c.setHelperText("helper");
        Assert.assertEquals("helper", c.getHelperText());

        c.setHelperText(null);
        Assert.assertNull(c.getHelperText());
    }

    @Test
    public void removeHelperComponent() {
        final HasHelperComponent c = new HasHelperComponent();
        final HasHelperComponent slotted = new HasHelperComponent();

        c.setHelperComponent(slotted);
        Assert.assertEquals(slotted, c.getHelperComponent());

        c.setHelperComponent(null);
        Assert.assertNull(c.getHelperComponent());
    }
}
