/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration.samplecode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.migration.samplecode.ShouldNotBeRewritten.MyStyleSheet;

@MyStyleSheet
public class ShouldNotBeRewritten extends Component {

    /**
     * This is not HtmlImport.
     */
    private static class MyHtmlImport {

    }

    /**
     * This is not StyleSheet.
     */
    @interface MyStyleSheet {

    }

}
