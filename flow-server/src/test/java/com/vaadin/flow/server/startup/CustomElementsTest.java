/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class CustomElementsTest {
    private CustomElements customElements;

    @Before
    public void setUp() {
        customElements = new CustomElements();
    }

    @Test
    public void addSingleElement() {
        addElementsAndCheckResults(
                Collections.singletonList(CustomElement.class),
                Collections.singletonList(CustomElement.class));
    }

    @Test(expected = IllegalStateException.class)
    public void addDifferentElements() {
        addElementsAndCheckResults(
                Arrays.asList(Tag2_Extend.class, Tag2_NotExtend.class),
                Arrays.asList(Tag2_Extend.class, Tag2_NotExtend.class));
    }

    @Test
    public void addExtendingElements_superclassFirst() {
        addElementsAndCheckResults(
                Arrays.asList(CustomElement.class, Tag1_Extend1.class,
                        Tag1_Extend2.class, Tag1_Extend3.class),
                Collections.singletonList(CustomElement.class));
    }

    @Test
    public void addExtendingElements_superclassLast() {
        addElementsAndCheckResults(
                Arrays.asList(Tag1_Extend1.class, Tag1_Extend2.class,
                        Tag1_Extend3.class, CustomElement.class),
                Collections.singletonList(CustomElement.class));
    }

    @Test
    public void addTwoExtendingWithDifferentTag() {
        addElementsAndCheckResults(
                Arrays.asList(Tag1_Extend1.class, Tag2_Extend.class),
                Arrays.asList(Tag1_Extend1.class, Tag2_Extend.class));
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    private void addElementsAndCheckResults(
            List<Class<? extends Component>> elementsToAdd,
            List<Class<? extends Component>> expectedClasses) {
        elementsToAdd.forEach(
                element -> customElements.addElement(getTag(element), element));

        MatcherAssert.assertThat(
                "Custom elements should contain only one class that we put into",
                customElements.computeTagToElementRelation(),
                is(expectedClasses.stream().collect(Collectors.toMap(
                        CustomElementsTest::getTag, Function.identity()))));
    }

    private static String getTag(Class<?> clazz) {
        return clazz.getAnnotation(Tag.class).value();
    }

    @Tag("custom-element")
    private static class CustomElement extends Component {
    }

    @Tag("custom-element")
    private static class Tag1_Extend1 extends CustomElement {
    }

    @Tag("custom-element")
    private static class Tag1_Extend2 extends CustomElement {
    }

    @Tag("custom-element")
    private static class Tag1_Extend3 extends CustomElement {
    }

    @Tag("different-element")
    private static class Tag2_Extend extends CustomElement {
    }

    @Tag("different-element")
    private static class Tag2_NotExtend extends CustomElement {
    }

}
