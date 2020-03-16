package com.vaadin.flow.data.provider;

import com.vaadin.flow.data.binder.HasDataProvider;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

/**
 * Test to ensure we allow using a generic wildcard for {@link HasDataProvider#setDataProvider(DataProvider)}.
 */
public class HasDataProviderTest {

    private static class Animal {}
    private static class Dog extends Animal {}

    @Test
    public void setDataProvider_generic_wildcard() {
        final HasDataProviderImpl<Animal> hasDataProviderImpl = new HasDataProviderImpl<>();

        final WildcardDataProvider<Animal> animalProvider = new WildcardDataProvider<>(Collections.emptyList());
        hasDataProviderImpl.setDataProvider(animalProvider);

        final WildcardDataProvider<Dog> dogProvider = new WildcardDataProvider<>(Collections.emptyList());
        hasDataProviderImpl.setDataProvider(dogProvider);
    }

    private static class HasDataProviderImpl<T> implements HasDataProvider<T> {

        @Override
        public void setDataProvider(final DataProvider<? extends T, ?> dataProvider) {
        }

        @Override
        public void setItems(final Collection<T> items) {
        }
    }
}
