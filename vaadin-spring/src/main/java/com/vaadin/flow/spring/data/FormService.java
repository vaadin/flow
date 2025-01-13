package com.vaadin.flow.spring.data;

/**
 * A service that can update and delete a given type of object.
 * 
 * @param <T>
 *            the type of object to manage
 * @param <ID>
 *            the type of the object's identifier
 * 
 */
public interface FormService<T, ID> {

    /**
     * Saves the given object and returns the (potentially) updated object.
     * <p>
     * If you store the object in a SQL database, the returned object might have
     * a new id or updated consistency version.
     *
     * @param value
     *            the object to save
     * @return the fresh object; will never be {@literal null}.
     */
    T save(T value);

    /**
     * Deletes the object with the given id.
     *
     * @param id
     *            the id of the object to delete
     */
    void delete(ID id);
}
