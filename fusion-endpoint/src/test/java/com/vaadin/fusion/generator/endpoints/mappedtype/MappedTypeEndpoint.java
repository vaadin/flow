/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.mappedtype;

import com.vaadin.fusion.Endpoint;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Endpoint
public class MappedTypeEndpoint {

    private Pageable pageable;

    public Pageable returnValue() {
        return PageRequest.of(2, 20);
    }

    public void parameter(Pageable pageable) {
        this.pageable = pageable;
    }

    public Pageable getPageable() {
        return pageable;
    }
}
