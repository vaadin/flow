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
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;

public class HasCorrectionTest {

    @Tag("div")
    public static class HasCorrectionComponent extends Component implements HasCorrection {

    }

    @Test
    public void defaultValue() {
        HasCorrectionComponent c = new HasCorrectionComponent();
        Assert.assertFalse(c.isAutocorrect());
    }

    @Test
    public void activateCorrection() {
        HasCorrectionComponent c = new HasCorrectionComponent();
        c.setAutocorrect(true);
        Assert.assertTrue(c.isAutocorrect());
    }

    @Test
    public void deactivateCorrection() {
        HasCorrectionComponent c = new HasCorrectionComponent();
        c.setAutocorrect(true);
        c.setAutocorrect(false);
    }
}
