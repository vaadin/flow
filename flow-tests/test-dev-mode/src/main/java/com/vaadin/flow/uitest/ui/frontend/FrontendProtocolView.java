/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wrapper view for the {@link FrontendProtocolTemplate} component. This class
 * is used by the FrontendProtocolIT to test the "frontend://" protocol in
 * multiple scenarios.
 *
 * @since 1.0
 */
@Route(value = "com.vaadin.flow.uitest.ui.frontend.FrontendProtocolView", layout = ViewTestLayout.class)
@Tag("div")
public class FrontendProtocolView extends Component implements HasComponents {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        FrontendProtocolTemplate component = new FrontendProtocolTemplate();
        add(component);
    }

}
