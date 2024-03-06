/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.scopes;

import org.springframework.beans.factory.ObjectFactory;

import com.vaadin.flow.server.VaadinSession;

public class TestBeanStore extends BeanStore {

    public TestBeanStore(VaadinSession session) {
        super(session);
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return super.get(name, objectFactory);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        super.registerDestructionCallback(name, callback);
    }

    @Override
    public Object remove(String name) {
        return super.remove(name);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

}
