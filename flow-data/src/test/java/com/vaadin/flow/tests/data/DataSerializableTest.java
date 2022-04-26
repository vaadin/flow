package com.vaadin.flow.tests.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.stream.Stream;

import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.testutil.ClassesSerializableTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DataSerializableTest extends ClassesSerializableTest {

    @Override
    protected Stream<String> getExcludedPatterns() {
        // JakartaBeanValidator is excluded by default because Jakarta
        // Validation dependency is added only on flow-data module, so remove it
        // from exclude list in order to check it is serializable
        return super.getExcludedPatterns().filter(
                pattern -> !pattern.endsWith(".JakartaBeanValidator.*"));
    }

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
