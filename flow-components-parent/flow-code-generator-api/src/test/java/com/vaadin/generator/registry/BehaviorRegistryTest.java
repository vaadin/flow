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
package com.vaadin.generator.registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasText;

/**
 * Unit tests for the {@link BehaviorRegistry}.
 *
 */
public class BehaviorRegistryTest {

    @Test
    public void nullOrEmptyProperties_returnEmptySet() {
        Set<Class<?>> set = BehaviorRegistry.getClassesForBehaviors(null);
        Assert.assertTrue("The set should be empty for null list of behaviors",
                set.isEmpty());

        set = BehaviorRegistry.getClassesForBehaviors(Collections.emptyList());
        Assert.assertTrue("The set should be empty for empty list of behaviors",
                set.isEmpty());
    }

    @Test
    public void notMappedProperties_returnEmptySet() {
        Set<Class<?>> set = BehaviorRegistry.getClassesForBehaviors(
                Arrays.asList("NOT_MAPPED_PROPERTY1", "NOT_MAPPED_PROPERTY2"));
        Assert.assertTrue(
                "The set should be empty for unmapped list of behaviors",
                set.isEmpty());
    }

    @Test
    public void clickableBehaviors() {
        assertClassIsPresent(ClickNotifier.class, "vaadin-button");
        assertClassIsPresent(ClickNotifier.class,
                "Polymer.PaperButtonBehavior");
        assertClassIsPresent(ClickNotifier.class,
                "Polymer.GestureEventListeners");
    }

    @Test
    public void hasTextBehaviors() {
        assertClassIsPresent(HasText.class, "vaadin-button");
        assertClassIsPresent(HasText.class, "paper-button");
    }

    private void assertClassIsPresent(Class<?> clazz, String selector) {
        Set<Class<?>> set = BehaviorRegistry
                .getClassesForBehaviors(Arrays.asList(selector));
        Assert.assertTrue(clazz.getSimpleName()
                + " should be in the set of inherited classes for selector "
                + selector, set.contains(clazz));
    }

}
