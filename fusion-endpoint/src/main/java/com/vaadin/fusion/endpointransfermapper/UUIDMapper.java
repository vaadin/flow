/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.endpointransfermapper;

import java.util.UUID;

import com.vaadin.fusion.endpointransfermapper.EndpointTransferMapper.Mapper;

/**
 * A mapper between {@link UUID} and {@link String}.
 */
public class UUIDMapper implements Mapper<UUID, String> {

    @Override
    public String toTransferType(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public UUID toEndpointType(String string) {
        return UUID.fromString(string);
    }

    @Override
    public Class<? extends UUID> getEndpointType() {
        return UUID.class;
    }

    @Override
    public Class<? extends String> getTransferType() {
        return String.class;
    }

}
