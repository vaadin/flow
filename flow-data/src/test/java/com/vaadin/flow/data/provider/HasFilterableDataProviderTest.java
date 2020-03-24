package com.vaadin.flow.data.provider;

import com.vaadin.flow.data.binder.HasFilterableDataProvider;
import com.vaadin.flow.function.SerializableFunction;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

/**
 * Test to ensure we allow using a generic wildcard for {@link HasFilterableDataProvider#setDataProvider(DataProvider)}.
 */
public class HasFilterableDataProviderTest {

    private static class Animal {}
    private static class Dog extends Animal{}

    @Test
    public void setDataProvider_generic_wildcard() {
        final HasFilterableDataProviderImpl<Animal, Void> hasDataProviderImpl = new HasFilterableDataProviderImpl<>();

        final WildcardDataProvider<Animal> animalProvider = new WildcardDataProvider<>(Collections.emptyList());
        hasDataProviderImpl.setDataProvider(animalProvider);

        final WildcardDataProvider<Dog> dogProvider = new WildcardDataProvider<>(Collections.emptyList());
        hasDataProviderImpl.setDataProvider(dogProvider);
    }

    private static class HasFilterableDataProviderImpl<T, F> implements HasFilterableDataProvider<T, F> {

        @Override
        public void setDataProvider(final DataProvider<? extends T, F> dataProvider) {
        }

        @Override
        public <C> void setDataProvider(final DataProvider<? extends T, C> dataProvider, SerializableFunction<F, C> filterConverter) {
        }

        @Override
        public void setItems(final Collection<T> items) {
        }
    }
}
