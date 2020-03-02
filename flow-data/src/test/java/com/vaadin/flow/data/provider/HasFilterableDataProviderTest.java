package com.vaadin.flow.data.provider;

import com.vaadin.flow.data.binder.HasFilterableDataProvider;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Test to ensure we allow using a generic wildcard for {@link HasFilterableDataProvider#setDataProvider(DataProvider)}.
 */
public class HasFilterableDataProviderTest {

    @Test
    public void setDataProvider_generic_wildcard() {
        ListDataProvider<ArrayList<String>> arrayListListDataProvider = new ListDataProvider<>(Collections.emptyList());
        HasFilterableDataProviderImpl<ArrayList<String>, SerializablePredicate<ArrayList<String>>> hasDataProviderImpl = new HasFilterableDataProviderImpl<>();
        hasDataProviderImpl.setDataProvider(arrayListListDataProvider);
    }

    private static class HasFilterableDataProviderImpl<T, F> implements HasFilterableDataProvider<T, F> {

        @Override
        public void setDataProvider(DataProvider<? extends T, F> dataProvider) {
        }

        @Override
        public <C> void setDataProvider(DataProvider<? extends T, C> dataProvider, SerializableFunction<F, C> filterConverter) {
        }

        @Override
        public void setItems(Collection<T> items) {
        }
    }
}
