package com.vaadin.flow.spring.data.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.spring.data.filter.PropertyStringFilter;

public class PropertyStringFilterSpecification<T> implements Specification<T> {

    private final PropertyStringFilter filter;

    public PropertyStringFilterSpecification(PropertyStringFilter filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {
        Path<?> propertyPath = getPath(filter.getPropertyId(), root);

        return toPredicate(root, criteriaBuilder, propertyPath);
    }

    /**
     * Converts a filter to a JPA predicate.
     *
     * @param root
     *            The root entity
     * @param criteriaBuilder
     *            The criteria builder
     * @param propertyPath
     *            The property path
     *
     */
    protected Predicate toPredicate(Root<T> root,
            CriteriaBuilder criteriaBuilder, Path<?> propertyPath) {
        Class<?> javaType = propertyPath.getJavaType();
        if (javaType == String.class) {
            return stringToPredicate(root, criteriaBuilder, filter,
                    (Path<String>) propertyPath);
        } else if (isNumber(javaType)) {
            return numberToPredicate(root, criteriaBuilder, filter,
                    (Path<String>) propertyPath);
        } else if (isBoolean(javaType)) {
            return booleanToPredicate(root, criteriaBuilder, filter,
                    (Path<String>) propertyPath);
        } else if (javaType == java.time.LocalDate.class) {
            return localDateToPredicate(root, criteriaBuilder, filter,
                    (Path<LocalDate>) propertyPath);
        } else if (javaType == LocalTime.class) {
            return localTimeToPredicate(root, criteriaBuilder, filter,
                    (Path<LocalTime>) propertyPath);
        } else if (javaType == java.time.LocalDateTime.class) {
            return localDateTimeToPredicate(root, criteriaBuilder, filter,
                    (Path<LocalDateTime>) propertyPath);
        } else if (javaType.isEnum()) {
            return enumToPredicate(root, criteriaBuilder, filter, propertyPath);

        }
        throw new IllegalArgumentException("No implementation for " + javaType
                + " using " + filter.getMatcher() + ".");
    }

    private static Predicate enumToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<?> propertyPath) {
        var enumValue = Enum.valueOf(
                propertyPath.getJavaType().asSubclass(Enum.class),
                filter.getFilterValue());

        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(propertyPath, enumValue);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "An enum cannot be filtered using contains");
        case GREATER_THAN:
            throw new IllegalArgumentException(
                    "An enum cannot be filtered using greater than");
        case LESS_THAN:
            throw new IllegalArgumentException(
                    "An enum cannot be filtered using less than");
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");
    }

    private static Predicate localTimeToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<LocalTime> propertyPath) {
        var timeValue = LocalTime.parse(filter.getFilterValue());
        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(propertyPath, timeValue);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "A time cannot be filtered using contains");
        case GREATER_THAN:
            return criteriaBuilder.greaterThan(propertyPath, timeValue);
        case LESS_THAN:
            return criteriaBuilder.lessThan(propertyPath, timeValue);
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");
    }

    private static Predicate localDateTimeToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<LocalDateTime> propertyPath) {
        var dateValue = LocalDate.parse(filter.getFilterValue());
        var minValue = LocalDateTime.of(dateValue, LocalTime.MIN);
        var maxValue = LocalDateTime.of(dateValue, LocalTime.MAX);
        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.between(propertyPath, minValue, maxValue);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "A datetime cannot be filtered using contains");
        case GREATER_THAN:
            return criteriaBuilder.greaterThan(propertyPath, maxValue);
        case LESS_THAN:
            return criteriaBuilder.lessThan(propertyPath, minValue);
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");
    }

    private static Predicate localDateToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<LocalDate> propertyPath) {
        var dateValue = LocalDate.parse(filter.getFilterValue());

        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(propertyPath, dateValue);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "A date cannot be filtered using contains");
        case GREATER_THAN:
            return criteriaBuilder.greaterThan(propertyPath, dateValue);
        case LESS_THAN:
            return criteriaBuilder.lessThan(propertyPath, dateValue);
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");

    }

    private static Predicate booleanToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<String> propertyPath) {
        Boolean booleanValue = Boolean.valueOf(filter.getFilterValue());
        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(propertyPath, booleanValue);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "A boolean cannot be filtered using contains");
        case GREATER_THAN:
            throw new IllegalArgumentException(
                    "A boolean cannot be filtered using greater than");
        case LESS_THAN:
            throw new IllegalArgumentException(
                    "A boolean cannot be filtered using less than");
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");
    }

    private static Predicate numberToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<String> propertyPath) {
        String value = filter.getFilterValue();
        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(propertyPath, value);
        case CONTAINS:
            throw new IllegalArgumentException(
                    "A number cannot be filtered using contains");
        case GREATER_THAN:
            return criteriaBuilder.greaterThan(propertyPath, value);
        case LESS_THAN:
            return criteriaBuilder.lessThan(propertyPath, value);
        }
        throw new IllegalArgumentException(
                "No implementation for " + filter.getMatcher() + ".");
    }

    private static Predicate stringToPredicate(Root<?> root,
            CriteriaBuilder criteriaBuilder, PropertyStringFilter filter,
            Path<String> propertyPath) {
        Expression<String> expr = criteriaBuilder
                .lower((Path<String>) propertyPath);
        var filterValueLowerCase = filter.getFilterValue().toLowerCase();
        switch (filter.getMatcher()) {
        case EQUALS:
            return criteriaBuilder.equal(expr, filterValueLowerCase);
        case CONTAINS:
            return criteriaBuilder.like(expr, "%" + filterValueLowerCase + "%");
        case GREATER_THAN:
            throw new IllegalArgumentException(
                    "A string cannot be filtered using greater than");
        case LESS_THAN:
            throw new IllegalArgumentException(
                    "A string cannot be filtered using less than");
        }
        throw new IllegalArgumentException(
                "Unknown matcher type: " + filter.getMatcher());
    }

    private Path<String> getPath(String propertyId, Root<T> root) {
        String[] parts = propertyId.split("\\.");
        Path<String> path = root.get(parts[0]);
        int i = 1;
        while (i < parts.length) {
            path = path.get(parts[i]);
            i++;
        }
        return path;
    }

    private boolean isNumber(Class<?> javaType) {
        return javaType == int.class || javaType == Integer.class
                || javaType == long.class || javaType == Long.class
                || javaType == float.class || javaType == Float.class
                || javaType == double.class || javaType == Double.class;
    }

    private boolean isBoolean(Class<?> javaType) {
        return javaType == boolean.class || javaType == Boolean.class;
    }

}
