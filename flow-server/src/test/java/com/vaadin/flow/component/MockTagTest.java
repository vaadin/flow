/*
 *
 *
 *  * Copyright 2000-2024 Vaadin Ltd.
 *
 *  *
 *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *
 *  * use this file except in compliance with the License. You may obtain a copy of
 *
 *  * the License at
 *
 *  *
 *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  *
 *
 *  * Unless required by applicable law or agreed to in writing, software
 *
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *
 *  * License for the specific language governing permissions and limitations under
 *
 *  * the License.
 *
 *
 */

package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import com.vaadin.flow.component.internal.ComponentMetaData;

public class MockTagTest {
    public static class MockComponent extends Component {
        @Tag("mock-tag")
        public static class Sample extends Component {}
    }

    public static class AnotherMockComponent extends Component {
        @Tag("another-tag")
        public static class Sample extends Component {}
    }
    private ComponentMetaData metaDataSample;
    private ComponentMetaData metaDataAnotherSample;

    @Before
    public void setup() {
        metaDataSample = new ComponentMetaData(MockComponent.Sample.class);
        metaDataAnotherSample = new ComponentMetaData(AnotherMockComponent.Sample.class);
    }
    @Test
    public void getComponentsByTag_correctlyMapsTags() {
        Set<Class<? extends Component>> components = metaDataSample.getComponentsByTag("mock-tag");
        Assert.assertTrue("The set should contain the MockComponent.Sample class",
                components.contains(MockComponent.Sample.class));

        Set<Class<? extends Component>> anotherComponents = metaDataAnotherSample.getComponentsByTag("another-tag");
        Assert.assertTrue("The set should contain the AnotherMockComponent.Sample class",
                anotherComponents.contains(AnotherMockComponent.Sample.class));
    }

    @Test
    public void getComponentsByTag_returnsEmptyForUnknownTag() {
        Set<Class<? extends Component>> components = metaDataSample.getComponentsByTag("non-existent-tag");
        Assert.assertTrue("The set should be empty for a non-existent tag", components.isEmpty());
    }
}
