/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;

/**
 * This is the Java wrapper for the webcomponent defined at the
 * frontend-protocol.html file. This component is tested by the
 * FrontendProtocolIT.
 *
 * @since 1.0
 */
@Tag("frontend-protocol")
@HtmlImport("frontend://components/frontend-protocol.html")
public class FrontendProtocolTemplate extends Component {
}
