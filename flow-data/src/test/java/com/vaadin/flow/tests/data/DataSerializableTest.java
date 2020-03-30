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
     * {@link com.vaadin.flow.data.provider.AbstractDataProvider#addDataProviderListener} may return a
     * {@link Registration} instance that is not deserializable due to self references.
     * This happens for example if the {@link DataProvider}, member of a component,
     * is used to add a {@link com.vaadin.flow.data.provider.DataProviderListener}
     * into an inner component; the resulting {@link Registration} handles a reference
     * to the {@link DataProvider} itself that is already referenced by the outer component
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
        public void setDataProvider(DataProvider<?, ?> dataProvider) {
            if (registration != null) {
                registration.remove();
            }
            registration = dataProvider.addDataProviderListener(event -> onDataProviderChange());
        }

        void onDataProviderChange() {

        }

    }

    static class Outer implements Serializable {
        private final ListDataProvider<Object> dataProvider = new ListDataProvider<>(Collections.emptyList());
        private final Inner inner = new Inner();

        public Outer() {
            inner.setDataProvider(dataProvider);
        }
    }
}
