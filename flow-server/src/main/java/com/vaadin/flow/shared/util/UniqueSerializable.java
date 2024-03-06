/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.util;

import java.io.Serializable;

/**
 * A base class for generating an unique object that is serializable.
 * <p>
 * This class is abstract but has no abstract methods to force users to create
 * an anonymous inner class. Otherwise each instance will not be unique.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class UniqueSerializable implements Serializable {

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        return getClass() == obj.getClass();
    }
}
