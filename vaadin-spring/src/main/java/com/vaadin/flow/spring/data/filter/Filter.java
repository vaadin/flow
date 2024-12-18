package com.vaadin.flow.spring.data.filter;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Superclass for all filters to be used with CRUD services. This specific class
 * is never used, instead a filter instance will be one of the following types:
 * <ul>
 * <li>{@link AndFilter} - Contains a list of nested filters, all of which need
 * to pass.</li>
 * <li>{@link OrFilter} - Contains a list of nested filters, of which at least
 * one needs to pass.</li>
 * <li>{@link PropertyStringFilter} - Matches a specific property, or nested
 * property path, against a filter value, using a specific operator.</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = OrFilter.class, name = "or"),
        @Type(value = AndFilter.class, name = "and"),
        @Type(value = PropertyStringFilter.class, name = "propertyString") })
public class Filter implements Serializable {

}
