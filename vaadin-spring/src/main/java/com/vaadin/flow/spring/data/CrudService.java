package com.vaadin.flow.spring.data;

/**
 * A service that can create, read, update, and delete a given type of object.
 */
public interface CrudService<T, ID> extends ListService<T>, FormService<T, ID> {
}
