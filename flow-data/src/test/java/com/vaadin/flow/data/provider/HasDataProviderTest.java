package com.vaadin.flow.data.provider;

import com.vaadin.flow.data.binder.HasDataProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Test to ensure we allow using a generic wildcard for {@link HasDataProvider#setDataProvider(DataProvider)}.
 */
public class HasDataProviderTest {

    @Test
    public void setDataProvider_generic_wildcard() {
        ListDataProvider<ArrayList<String>> arrayListListDataProvider = new ListDataProvider<>(Collections.emptyList());
        HasDataProviderImpl<List<String>> hasDataProviderImpl = new HasDataProviderImpl<>();
        hasDataProviderImpl.setDataProvider(arrayListListDataProvider);
    }

    private static class HasDataProviderImpl<T> implements HasDataProvider<T> {

        @Override
        public void setDataProvider(DataProvider<? extends T, ?> dataProvider) {
        }

        @Override
        public void setItems(Collection<T> items) {
        }
    }
}
