/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;

class VaadinSpringDataHelpersTest {

    @Test
    public void toSpringDataSort_generatesAscendingAndDescendingSpringSort() {
        List<QuerySortOrder> querySortOrders = QuerySortOrder.asc("name")
                .thenDesc("age").build();
        Query<?, ?> query = new Query<>(0, 1, querySortOrders, null, null);

        Sort sort = VaadinSpringDataHelpers.toSpringDataSort(query);

        Assertions.assertNotNull(sort);
        Assertions.assertEquals(2L, sort.stream().count());

        Sort.Order nameOrder = sort.getOrderFor("name");
        Assertions.assertNotNull(nameOrder);
        Assertions.assertTrue(nameOrder.isAscending());

        Sort.Order ageOrder = sort.getOrderFor("age");
        Assertions.assertNotNull(ageOrder);
        Assertions.assertTrue(ageOrder.isDescending());
    }

    @Test
    public void toSpringPageRequest_generatesSpringPageRequestWithPagingAndSort() {
        List<QuerySortOrder> querySortOrders = QuerySortOrder.asc("name")
                .build();
        Query<?, ?> query = new Query<>(100, 50, querySortOrders, null, null);

        PageRequest pageRequest = VaadinSpringDataHelpers
                .toSpringPageRequest(query);

        Assertions.assertNotNull(pageRequest);
        Assertions.assertEquals(50, pageRequest.getPageSize());
        Assertions.assertEquals(2, pageRequest.getPageNumber());

        Sort.Order order = pageRequest.getSort().getOrderFor("name");
        Assertions.assertNotNull(order);
        Assertions.assertTrue(order.isAscending());
    }

    @Test
    public void fromPagingRepository_fetchesItemsFromPagingRepoAccordingToVaadinQuery() {
        PagingAndSortingRepository repo = Mockito
                .mock(PagingAndSortingRepository.class);

        Mockito.doAnswer(mock -> {
            PageRequest pageRequest = mock.getArgument(0);
            int from = pageRequest.getPageNumber() * pageRequest.getPageSize();

            Sort.Order order = pageRequest.getSort()
                    .getOrderFor("someSortField");
            Stream<Integer> itemsStream = IntStream.range(0, 500).boxed();

            if (order.isDescending()) {
                itemsStream = itemsStream.sorted(Collections.reverseOrder());
            }
            // given string items 'Item XYZ' ordered according to a given query
            List<String> items = itemsStream.skip(from)
                    .limit(pageRequest.getPageSize()).map(i -> "Item " + i)
                    .collect(Collectors.toList());
            return new PageImpl<>(items);
        }).when(repo).findAll(Mockito.any(PageRequest.class));

        CallbackDataProvider.FetchCallback<String, Void> callback = VaadinSpringDataHelpers
                .fromPagingRepository(repo);

        // when items with indexes 100..149 and descending order are fetched
        Query<String, Void> query = new Query<>(100, 50,
                QuerySortOrder.desc("someSortField").build(), null, null);
        List<String> result = callback.fetch(query)
                .collect(Collectors.toList());

        // then the result should contain items 'Item 399'...'Item 350'.
        Assertions.assertEquals(50, result.size());
        Assertions.assertEquals("Item 399", result.get(0));
        Assertions.assertEquals("Item 350", result.get(49));
    }
}
