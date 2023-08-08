/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;

@NpmPackage(value = "@polymer/paper-input", version = "3.2.1")
@NpmPackage(value = "@polymer/paper-checkbox", version = "3.0.1")
public class NpmPackageLayout extends Div implements RouterLayout {
}
