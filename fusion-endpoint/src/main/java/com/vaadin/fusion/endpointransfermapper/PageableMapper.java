/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.endpointransfermapper;

import com.vaadin.fusion.endpointransfermapper.EndpointTransferMapper.Mapper;
import com.vaadin.fusion.mappedtypes.Pageable;

import org.springframework.data.domain.PageRequest;

/**
 * A mapper between {@link Pageable} and {@link Pageable}.
 */
public class PageableMapper
        implements Mapper<org.springframework.data.domain.Pageable, Pageable> {

    private SortMapper sortMapper = new SortMapper();

    @Override
    public Class<? extends org.springframework.data.domain.Pageable> getEndpointType() {
        return org.springframework.data.domain.Pageable.class;
    }

    @Override
    public Class<? extends Pageable> getTransferType() {
        return Pageable.class;
    }

    @Override
    public Pageable toTransferType(
            org.springframework.data.domain.Pageable pageable) {
        Pageable transferPageable = new Pageable();
        transferPageable.setPageNumber(pageable.getPageNumber());
        transferPageable.setPageSize(pageable.getPageSize());
        transferPageable.setSort(sortMapper.toTransferType(pageable.getSort()));

        return transferPageable;
    }

    @Override
    public org.springframework.data.domain.Pageable toEndpointType(
            Pageable transferPageable) {
        org.springframework.data.domain.Sort sort = sortMapper
                .toEndpointType(transferPageable.getSort());
        return PageRequest.of(transferPageable.getPageNumber(),
                transferPageable.getPageSize(), sort);
    }
}
