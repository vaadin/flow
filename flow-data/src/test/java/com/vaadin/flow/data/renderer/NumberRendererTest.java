/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.data.renderer;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.ValueProvider;

public class NumberRendererTest {

    @Test
    public void getFormattedValue_numberIsFormattedUsingLocale() {
        NumberRenderer<Number> renderer = new NumberRenderer<>(
                ValueProvider.identity(), Locale.GERMANY);

        String formatted = renderer.getFormattedValue(1.2);
        Assert.assertEquals("1,2", formatted);

        renderer = new NumberRenderer<>(ValueProvider.identity(),
                Locale.ENGLISH);

        formatted = renderer.getFormattedValue(1.2);
        Assert.assertEquals("1.2", formatted);
    }

}
