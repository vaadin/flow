/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.gizmo;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.gizmo.DevModeGizmoView")
public class DevModeGizmoView extends Div {
    public DevModeGizmoView() {
        add(new Paragraph("This is a dev mode gizmo test"));
    }
}
