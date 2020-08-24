package com.vaadin.flow.server.connect.generator.endpoints.superclassmethods;

import java.util.Optional;

import com.vaadin.flow.server.connect.EndpointExposed;

@EndpointExposed
public abstract class CrudEndpoint<T, ID> extends ReadOnlyEndpoint<T, ID> {
    public T update(T entity) {
        return entity;
    }

    public void delete(ID id) {
    }
}
