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
package com.vaadin.flow.tests.data;

import java.io.Serializable;
import java.util.Collections;

import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.testutil.ClassesSerializableTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DataSerializableTest extends ClassesSerializableTest {

    /*
     * AbstractDataProvider.addDataProviderListener may return a Registration
     * instance that is not deserializable due to self references. This happens
     * for example if the dataprovider, member of a component, is used to add a
     * com.vaadin.flow.data.provider.DataProviderListener into an inner
     * component; the resulting Registration handles a reference to the
     * dataprovider itself that is already referenced by the outer component
     */
    @Test
    public void selfReferenceSerialization() throws Throwable {
        Outer outer = new Outer();
        Outer out = serializeAndDeserialize(outer);
        assertNotNull(out);
    }

    static class Inner implements HasDataProvider<Object>, Serializable {

        private Registration registration;

        @Override
        public void setDataProvider(DataProvider<Object, ?> dataProvider) {
            if (registration != null) {
                registration.remove();
            }
            registration = dataProvider
                    .addDataProviderListener(event -> onDataProviderChange());
        }

        void onDataProviderChange() {

        }

    }

    static class Outer implements Serializable {
        private final ListDataProvider<Object> dataProvider = new ListDataProvider<>(
                Collections.emptyList());
        private final Inner inner = new Inner();

        public Outer() {
            inner.setDataProvider(dataProvider);
        }
    }
}
