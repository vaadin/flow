package com.vaadin.flow.spring.data.jpa;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.googlecode.gentyref.GenericTypeReflector;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import com.vaadin.flow.spring.data.CountService;
import com.vaadin.flow.spring.data.GetService;
import com.vaadin.flow.spring.data.ListService;
import com.vaadin.flow.spring.data.filter.Filter;

/**
 * A service that delegates list operations to a JPA repository.
 *
 * @param <T>
 *            the type of object to list
 * @param <ID>
 *            the type of the object's identifier
 * @param <R>
 *            the type of the JPA repository
 */
public class ListRepositoryService<T, ID, R extends CrudRepository<T, ID> & JpaSpecificationExecutor<T>>
        implements ListService<T>, GetService<T, ID>, CountService,
        ApplicationContextAware {

    // will store the max page size as configured in
    // `spring.data.rest.pageable.max-page-size` if available
    private static Integer maxPageSize;

    private R repository;

    private final Class<T> entityClass;

    /**
     * Creates the service using the given repository and filter converter.
     *
     * @param repository
     *            the JPA repository
     */
    public ListRepositoryService(R repository) {
        this.repository = repository;
        this.entityClass = resolveEntityClass();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        if (maxPageSize == null) {
            var env = applicationContext.getEnvironment();
            maxPageSize = env.getProperty(
                    "spring.data.rest.pageable.max-page-size", Integer.class);
        }
    }

    /**
     * Accessor for the repository instance.
     *
     * @return the repository instance
     */
    protected R getRepository() {
        return repository;
    }

    @Deprecated
    protected void internalSetRepository(R repository) {
        // Only for Hilla backwards compatibility
        this.repository = repository;
    }

    @Override
    public List<T> list(Pageable pageable, @Nullable Filter filter) {
        if (maxPageSize != null && pageable.getPageSize() > maxPageSize) {
            pageable = PageRequest.of(pageable.getPageNumber(), maxPageSize,
                    pageable.getSort());
        }

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
        return JpaFilterConverter.toSpec(filter, entityClass,
                PropertyStringFilterSpecification::new);
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
