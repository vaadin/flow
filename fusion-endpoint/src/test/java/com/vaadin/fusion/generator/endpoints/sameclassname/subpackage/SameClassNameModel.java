/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.sameclassname.subpackage;

public class SameClassNameModel {
    String bar;
    SubProperty foofoo;
    com.vaadin.fusion.generator.endpoints.sameclassname.subpackage.SubProperty barbarfoo;

    public static class SubProperty {
        String foobar;
    }
}
