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
package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;

public class HasOrderedComponentsTest {

    static class TestOrderedComponents implements HasOrderedComponents {

        @Override
        public Element getElement() {
            return null;
        }

        @Override
        public Stream<Component> getChildren() {
            return null;
        }

    }

    @Tag("div")
    static class TestComponentContianer extends Component
            implements HasOrderedComponents {

    }

    @Tag(Tag.A)
    static class Anchor extends Component {

    }

    private HasOrderedComponents components = Mockito
            .spy(TestOrderedComponents.class);

    private TestComponentContianer contianer = new TestComponentContianer();

    @Test
    public void indexOf_componentIsChild_returnsIndexOfChild() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class), comp,
                        Mockito.mock(Component.class)).stream());

        Assert.assertEquals(1, components.indexOf(comp));

        contianer = new TestComponentContianer();
        comp = new Anchor();
        contianer.add(new Text(""), comp);
        Assert.assertEquals(1, contianer.indexOf(comp));
    }

    @Test
    public void indexOf_componentIsNotChild_returnsNegative() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());

        Assert.assertEquals(-1, components.indexOf(comp));
    }

    @Test(expected = IllegalArgumentException.class)
    public void indexOf_componentIsNull_throws() {
        Mockito.when(components.getChildren()).thenReturn(Stream.empty());

        components.indexOf(null);
    }

    @Test
    public void getComponentCount_returnsChildrenSize() {
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());
        Assert.assertEquals(2, components.getComponentCount());

        contianer = new TestComponentContianer();
        contianer.add(new Text(""), new Anchor());
        Assert.assertEquals(2, contianer.getComponentCount());
    }

    @Test
    public void getComponentAt_returnsComponentAtIndex() {
        Component comp = Mockito.mock(Component.class);
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class), comp,
                        Mockito.mock(Component.class)).stream());

        Assert.assertSame(comp, components.getComponentAt(1));

        contianer = new TestComponentContianer();
        comp = new Anchor();
        contianer.add(new Text(""), comp);
        Assert.assertSame(comp, contianer.getComponentAt(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getComponentAt_negativeIndex_throws() {
        Mockito.when(components.getChildren())
                .thenReturn(Arrays.asList(Mockito.mock(Component.class),
                        Mockito.mock(Component.class)).stream());

        components.getComponentAt(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getComponentAt_indexIsGreaterThanSize_throws() {
        Mockito.when(components.getChildren())
                .thenReturn(Stream.of(Mockito.mock(Component.class)));

        components.getComponentAt(2);
    }
}
