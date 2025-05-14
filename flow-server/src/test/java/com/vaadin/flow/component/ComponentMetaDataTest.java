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
package com.vaadin.flow.component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.internal.ComponentMetaData;
import com.vaadin.flow.component.internal.ComponentMetaData.SynchronizedPropertyInfo;
import com.vaadin.flow.dom.DisabledUpdateMode;

public class ComponentMetaDataTest {

    @Tag(Tag.A)
    public static class Sample extends Component {

        @Synchronize(value = "bar", property = "baz")
        public String getFoo() {
            return null;
        }

        public String getBar() {
            return null;
        }
    }

    public interface HasFoo {
        @Synchronize(value = "bar", property = "baz", allowUpdates = DisabledUpdateMode.ALWAYS)
        String getFoo();
    }

    public static class SubClass extends Sample {

        @Override
        public String getFoo() {
            return null;
        }

        @Synchronize(value = "foo", property = "bar")
        public String getBaz() {
            return null;
        }
    }

    public static class ChangeSyncProperty extends Sample {

        @Override
        @Synchronize(value = "baz", property = "foo")
        public String getFoo() {
            return null;
        }
    }

    @Tag(Tag.A)
    public static class HasFooImpl extends Component implements HasFoo {

        @Override
        public String getFoo() {
            return null;
        }
    }

    public static class HasFooProtected extends Component {
        @Synchronize(value = "bar", property = "baz")
        protected String getFoo() {
            return null;
        }
    }

    public static class HasFooPackagePrivate extends Component {
        @Synchronize(value = "bar", property = "baz")
        String getFoo() {
            return null;
        }
    }

    public static class HasFooPrivate extends Component {
        @Synchronize(value = "bar", property = "baz")
        private String getFoo() {
            return null;
        }
    }

    @Test
    public void synchronizedProperties_methodInClass() {
        assertFooProperty(Sample.class);
    }

    @Test
    public void synchronizedProperties_methodInInterface() {
        assertFooProperty(HasFooImpl.class, DisabledUpdateMode.ALWAYS);
    }

    @Test
    public void synchronizedProperties_protectedMethod() {
        assertFooProperty(HasFooProtected.class);
    }

    @Test
    public void synchronizedProperties_packagePrivateMethod() {
        assertFooProperty(HasFooPackagePrivate.class);
    }

    @Test
    public void synchronizedProperties_privateMethod() {
        assertFooProperty(HasFooPrivate.class);
    }

    @Test
    public void synchronizedProperties_hasOverriddenMethod() {
        ComponentMetaData data = new ComponentMetaData(SubClass.class);

        Collection<SynchronizedPropertyInfo> props = data
                .getSynchronizedProperties();
        Assert.assertEquals(2, props.size());

        List<SynchronizedPropertyInfo> bazProps = props.stream()
                .filter(prop -> prop.getProperty().equals("baz"))
                .collect(Collectors.toList());
        Assert.assertEquals(1, bazProps.size());
        SynchronizedPropertyInfo info = bazProps.get(0);

        List<String> events = info.getEventNames().collect(Collectors.toList());
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("bar", events.get(0));

        Assert.assertTrue(props.stream()
                .anyMatch(prop -> prop.getProperty().equals("bar")));
    }

    @Test
    public void synchronizedProperties_overridesMethodAndProperty() {
        ComponentMetaData data = new ComponentMetaData(
                ChangeSyncProperty.class);

        Collection<SynchronizedPropertyInfo> props = data
                .getSynchronizedProperties();
        Assert.assertEquals(1, props.size());

        SynchronizedPropertyInfo info = props.iterator().next();
        Assert.assertEquals("foo", info.getProperty());

        List<String> events = info.getEventNames().collect(Collectors.toList());
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("baz", events.get(0));
    }

    private void assertFooProperty(Class<? extends Component> clazz) {
        assertFooProperty(clazz, DisabledUpdateMode.ONLY_WHEN_ENABLED);
    }

    private void assertFooProperty(Class<? extends Component> clazz,
            DisabledUpdateMode mode) {
        ComponentMetaData data = new ComponentMetaData(clazz);

        Collection<SynchronizedPropertyInfo> props = data
                .getSynchronizedProperties();
        Assert.assertEquals(1, props.size());

        SynchronizedPropertyInfo info = props.iterator().next();
        Assert.assertEquals("baz", info.getProperty());

        Assert.assertEquals(mode, info.getUpdateMode());

        List<String> events = info.getEventNames().collect(Collectors.toList());
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("bar", events.get(0));
    }
}
