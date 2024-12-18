package com.vaadin.flow.spring.data.jpa;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.googlecode.gentyref.GenericTypeReflector;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import com.vaadin.flow.Nullable;
import com.vaadin.flow.spring.data.CountService;
import com.vaadin.flow.spring.data.GetService;
import com.vaadin.flow.spring.data.ListService;
import com.vaadin.flow.spring.data.filter.Filter;

/**
 * A service that delegates list operations to a JPA repository.
 */
public class ListRepositoryService<T, ID, R extends CrudRepository<T, ID> & JpaSpecificationExecutor<T>>
        implements ListService<T>, GetService<T, ID>, CountService {

    private final JpaFilterConverter jpaFilterConverter;

    private R repository;

    private final Class<T> entityClass;

    /**
     * Creates the service using the given repository.
     *
     * @param repository
     *            the JPA repository
     */
    public ListRepositoryService(R repository,
            JpaFilterConverter jpaFilterConverter) {
        this.jpaFilterConverter = jpaFilterConverter;
        this.repository = repository;
        this.entityClass = resolveEntityClass();
    }

    /**
     * Accessor for the repository instance.
     *
     * @return the repository instance
     */
    protected R getRepository() {
        return repository;
    }

    @Override
    public List<T> list(Pageable pageable, @Nullable Filter filter) {
        Specification<T> spec = toSpec(filter);
        return getRepository().findAll(spec, pageable).getContent();
    }

    @Override
    public Optional<T> get(ID id) {
        return getRepository().findById(id);
    }

    @Override
    public boolean exists(ID id) {
        return getRepository().existsById(id);
    }

    /**
     * Counts the number of entities that match the given filter.
     *
     * @param filter
     *            the filter, or {@code null} to use no filter
     * @return
     */
    @Override
    public long count(@Nullable Filter filter) {
        return getRepository().count(toSpec(filter));
    }

    /**
     * Converts the given filter to a JPA specification.
     *
     * @param filter
     *            the filter to convert
     * @return a JPA specification
     */
    protected Specification<T> toSpec(@Nullable Filter filter) {
        return jpaFilterConverter.toSpec(filter, entityClass);
    }

    @SuppressWarnings("unchecked")
    protected Class<T> resolveEntityClass() {
        var entityTypeParam = ListRepositoryService.class
                .getTypeParameters()[0];
        Type entityType = GenericTypeReflector.getTypeParameter(getClass(),
                entityTypeParam);
        if (entityType == null) {
            throw new IllegalStateException(String.format(
                    "Unable to detect the type for the class '%s' in the "
                            + "class '%s'.",
                    entityTypeParam, getClass()));
        }
        return (Class<T>) GenericTypeReflector.erase(entityType);
    }
}
