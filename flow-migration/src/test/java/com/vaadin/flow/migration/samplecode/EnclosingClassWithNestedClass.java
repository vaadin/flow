/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration.samplecode;

import java.io.Serializable;
import java.util.List;

public class EnclosingClassWithNestedClass {

    public abstract static class NestedComponent<T extends List<? extends Number> & Serializable, U extends Object>
            extends GenericComponent<T, U> {

    }

}
