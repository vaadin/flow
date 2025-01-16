package com.vaadin.flow.spring.data;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;

import com.vaadin.flow.spring.data.filter.Filter;

/**
 * A service that can list the given type of object.
 *
 * @param <T>
 *            the type of object to list
 */
public interface ListService<T> {
    /**
     * Lists objects of the given type using the paging and sorting options
     * provided in the parameters.
     *
     * @param pageable
     *            contains information about paging and sorting
     * @return a list of objects or an empty list if no objects were found
     */
    default List<T> list(Pageable pageable) {
        return list(pageable, null);
    }

    /**
     * Lists objects of the given type using the paging, sorting and filtering
     * options provided in the parameters.
     *
     * @param pageable
     *            contains information about paging and sorting
     * @param filter
     *            the filter to apply or {@code null} to not filter
     * @return a list of objects or an empty list if no objects were found
     */
    List<T> list(Pageable pageable, @Nullable Filter filter);

}
