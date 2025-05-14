/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring.data;

import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import java.io.Serializable;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Contains helper methods to work with Spring Data based back-ends and Vaadin
 * components.
 */
public interface VaadinSpringDataHelpers extends Serializable {

    /**
     * Translates given Query object from a Vaadin component to Spring Data Sort
     * object.
     * <p>
     * Can be used as a helper when making a lazy data binding from a Vaadin
     * component to a Spring Data based back-end. The method expects Vaadin sort
     * data to include the property name.
     *
     * @param vaadinQuery
     *            the Vaadin Query object passed by the component
     * @return the Sort object that can be passed for Spring Data based back-end
     */
    static Sort toSpringDataSort(Query<?, ?> vaadinQuery) {
        return Sort.by(vaadinQuery.getSortOrders().stream()
                .map(so -> so.getDirection() == SortDirection.ASCENDING
                        ? Order.asc(so.getSorted())
                        : Order.desc(so.getSorted()))
                .collect(Collectors.toList()));
    }

    /**
     * Creates a Spring Data {@link PageRequest} based on the Vaadin
     * {@link Query} object. Takes sort into account, based on properties.
     *
     * @param vaadinQuery
     *            the query object from Vaadin component
     * @return a {@link PageRequest} that can be passed for Spring Data based
     *         back-end
     */
    static PageRequest toSpringPageRequest(Query<?, ?> vaadinQuery) {
        Sort sort = VaadinSpringDataHelpers.toSpringDataSort(vaadinQuery);
        return PageRequest.of(vaadinQuery.getPage(), vaadinQuery.getPageSize(),
                sort);
    }

    /**
     * Binds all items from a given paging Spring Data repository to
     * {@code Grid}. Usage example:
     * <p>
     * {@code grid.setItems(fromPagingRepository(repo));}
     * <p>
     *
     * @param <T>
     *            the type of items to bind
     * @param repo
     *            the repository where the results should be fetched from
     * @return the FetchCallback that makes the lazy binding to {@code Grid}.
     */
    static <T> FetchCallback<T, Void> fromPagingRepository(
            PagingAndSortingRepository<T, ?> repo) {
        return query -> repo.findAll(toSpringPageRequest(query)).stream();
    }

}
