package com.vaadin.flow.spring.data.jpa;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.spring.data.filter.AndFilter;
import com.vaadin.flow.spring.data.filter.Filter;
import com.vaadin.flow.spring.data.filter.OrFilter;
import com.vaadin.flow.spring.data.filter.PropertyStringFilter;

/**
 * Utility class for converting {@link Filter} specifications into JPA filter
 * specifications. This class can be used to implement filtering for custom
 * {@link com.vaadin.flow.spring.data.ListService} or
 * {@link com.vaadin.flow.spring.data.CrudService} implementations that use JPA
 * as the data source.
 */
public final class JpaFilterConverter {

    private JpaFilterConverter() {
        // Utilities only
    }

    /**
     * Converts the given filter specification into a JPA filter specification
     * for the specified entity class.
     * <p>
     * If the filter contains {@link PropertyStringFilter} instances, their
     * properties, or nested property paths, need to match the structure of the
     * entity class. Likewise, their filter values should be in a format that
     * can be parsed into the type that the property is of.
     *
     * @param <T>
     *            the type of the entity
     * @param rawFilter
     *            the filter to convert
     * @param entity
     *            the entity class
     * @param propertyStringFilterSupplier
     *            a function that can convert a PropertyStringFilter into a JPA
     *            filter specification
     * @return a JPA filter specification for the given filter
     */
    public static <T> Specification<T> toSpec(Filter rawFilter, Class<T> entity,
            Function<PropertyStringFilter, Specification<T>> propertyStringFilterSupplier) {
        if (rawFilter == null) {
            return Specification.anyOf();
        }
        if (rawFilter instanceof AndFilter filter) {
            return Specification.allOf(filter.getChildren().stream()
                    .map(f -> toSpec(f, entity, propertyStringFilterSupplier))
                    .toList());
        } else if (rawFilter instanceof OrFilter filter) {
            return Specification.anyOf(filter.getChildren().stream()
                    .map(f -> toSpec(f, entity, propertyStringFilterSupplier))
                    .toList());
        } else if (rawFilter instanceof PropertyStringFilter filter) {
            return propertyStringFilterSupplier.apply(filter);
        } else {
            throw new IllegalArgumentException(
                    "Unknown filter type " + rawFilter.getClass().getName());
        }
    }
}
