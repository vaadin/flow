package com.vaadin.flow.spring.data.jpa;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import com.vaadin.flow.spring.data.CrudService;

/**
 * A service that delegates crud operations to a JPA repository.
 *
 * @param <T>
 *            the type of object to manage
 * @param <ID>
 *            the type of the object's identifier
 * @param <R>
 *            the type of the JPA repository
 *
 */
public class CrudRepositoryService<T, ID, R extends CrudRepository<T, ID> & JpaSpecificationExecutor<T>>
        extends ListRepositoryService<T, ID, R> implements CrudService<T, ID> {

    /**
     * Creates the service using the given repository.
     *
     * @param repository
     *            the JPA repository
     */
    public CrudRepositoryService(R repository) {
        super(repository);
    }

    @Override
    public @Nullable T save(T value) {
        return getRepository().save(value);
    }

    /**
     * Saves the given objects and returns the (potentially) updated objects.
     * <p>
     * The returned objects might have new ids or updated consistency versions.
     *
     * @param values
     *            the objects to save
     * @return the fresh objects
     */
    public List<T> saveAll(Iterable<T> values) {
        List<T> saved = new ArrayList<>();
        getRepository().saveAll(values).forEach(saved::add);
        return saved;
    }

    @Override
    public void delete(ID id) {
        getRepository().deleteById(id);
    }

    /**
     * Deletes the objects with the given ids.
     *
     * @param ids
     *            the ids of the objects to delete
     */
    public void deleteAll(Iterable<ID> ids) {
        getRepository().deleteAllById(ids);
    }

}
