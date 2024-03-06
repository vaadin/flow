/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.testbench.HasElementQuery;
import com.vaadin.testbench.TestBenchElement;

public interface HasById {
    default TestBenchElement byId(String id, String... childIds) {
        HasElementQuery _this = (HasElementQuery) this;
        return byId(_this, id, childIds);
    }

    default TestBenchElement byId(HasElementQuery elementQuery, String id,
            String... childIds) {
        TestBenchElement testBenchElement = elementQuery
                .$(TestBenchElement.class).id(id);
        if (testBenchElement != null && childIds.length > 0) {
            for (String childId : childIds) {
                testBenchElement = testBenchElement.$(TestBenchElement.class)
                        .id(childId);
                if (testBenchElement == null) {
                    return null;
                }
            }
        }
        return testBenchElement;
    }
}
