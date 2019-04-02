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
package com.vaadin.flow.di;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

public class DefaultInstantiatorTest {

    @Tag(Tag.A)
    public static class TestComponent extends Component {

    }

    @Test
    public void createComponent_dontDependOnGetOrCreate() {
        DefaultInstantiator instantiator = Mockito
                .mock(DefaultInstantiator.class);

        Mockito.when(instantiator.createComponent(Mockito.any()))
                .thenCallRealMethod();

        TestComponent component = instantiator
                .createComponent(TestComponent.class);

        Assert.assertNotNull(component);

        Mockito.verify(instantiator, Mockito.times(0))
                .getOrCreate(Mockito.any());
    }
}
