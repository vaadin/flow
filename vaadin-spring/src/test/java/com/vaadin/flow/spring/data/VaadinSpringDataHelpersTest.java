/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.data;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;

public class VaadinSpringDataHelpersTest {

    @Test
    public void toSpringDataSort_generatesAscendingAndDescendingSpringSort() {
        List<QuerySortOrder> querySortOrders = QuerySortOrder.asc("name")
                .thenDesc("age").build();
        Query<?, ?> query = new Query<>(0, 1, querySortOrders, null, null);

        Sort sort = VaadinSpringDataHelpers.toSpringDataSort(query);

        Assert.assertNotNull(sort);
        Assert.assertEquals(2L, sort.stream().count());

        Sort.Order nameOrder = sort.getOrderFor("name");
        Assert.assertNotNull(nameOrder);
        Assert.assertTrue(nameOrder.isAscending());

        Sort.Order ageOrder = sort.getOrderFor("age");
        Assert.assertNotNull(ageOrder);
        Assert.assertTrue(ageOrder.isDescending());
    }

    @Test
    public void toSpringPageRequest_generatesSpringPageRequestWithPagingAndSort() {
        List<QuerySortOrder> querySortOrders = QuerySortOrder.asc("name")
                .build();
        Query<?, ?> query = new Query<>(100, 50, querySortOrders, null, null);

        PageRequest pageRequest = VaadinSpringDataHelpers
                .toSpringPageRequest(query);

        Assert.assertNotNull(pageRequest);
        Assert.assertEquals(50, pageRequest.getPageSize());
        Assert.assertEquals(2, pageRequest.getPageNumber());

        Sort.Order order = pageRequest.getSort().getOrderFor("name");
        Assert.assertNotNull(order);
        Assert.assertTrue(order.isAscending());
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
        Assert.assertEquals(50, result.size());
        Assert.assertEquals("Item 399", result.get(0));
        Assert.assertEquals("Item 350", result.get(49));
    }
}
