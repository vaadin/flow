/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.endpointransfermapper;

import java.util.List;

import com.vaadin.fusion.endpointransfermapper.EndpointTransferMapper.Mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * A mapper between {@link Page} and {@link List}.
 */
public class PageMapper implements Mapper<Page<?>, List<?>> {

    @Override
    public Class<? extends Page<?>> getEndpointType() {
        return (Class) Page.class;
    }

    @Override
    public Class<? extends List<?>> getTransferType() {
        return (Class) List.class;
    }

    @Override
    public List toTransferType(Page page) {
        return page.getContent();
    }

    @Override
    public Page toEndpointType(List list) {
        return new PageImpl(list);
    }
}
