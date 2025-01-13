package com.vaadin.flow.spring.data;

/**
 * A service that can create, read, update, and delete a given type of object.
 * 
 * @param <T>
 *            the type of object to manage
 * @param <ID>
 *            the type of the object's identifier
 */
public interface CrudService<T, ID> extends ListService<T>, FormService<T, ID> {
}
