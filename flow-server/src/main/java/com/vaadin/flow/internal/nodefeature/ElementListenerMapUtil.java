/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.Set;

public class ElementListenerMapUtil {

    public static Set<String> getExpressions(ElementListenerMap map,
            String name) {
        return map.getExpressions(name);
    }
}
