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
package com.vaadin.data;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Checkbox;

public class CheckBoxTest {

    @Test
    public void initialValueCtor() {
        Checkbox checkbox = new Checkbox(true);
        Assert.assertTrue(checkbox.getValue());

        checkbox = new Checkbox(false);
        Assert.assertFalse(checkbox.getValue());
    }

    @Test
    public void labelAndInitialValueCtor() {
        Checkbox checkbox = new Checkbox("foo", true);
        Assert.assertTrue(checkbox.getValue());
        Assert.assertEquals("foo", checkbox.getLabel());

        checkbox = new Checkbox("foo", false);
        Assert.assertFalse(checkbox.getValue());
        Assert.assertEquals("foo", checkbox.getLabel());
    }

}
