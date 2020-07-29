package com.vaadin.flow.server.connect.generator.endpoints.superclassmethods;

import java.util.Optional;

import com.vaadin.flow.server.connect.EndpointExposed;

@EndpointExposed
public abstract class CrudEndpoint<T, ID> {
    public Optional<T> get(ID id) {
        return Optional.ofNullable(null);
    }

    public T update(T entity) {
        return entity;
    }

    public void delete(ID id) {
    }
    
}