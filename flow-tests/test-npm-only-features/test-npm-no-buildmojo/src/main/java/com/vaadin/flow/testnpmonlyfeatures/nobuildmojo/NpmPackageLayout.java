package com.vaadin.flow.testnpmonlyfeatures.nobuildmojo;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;

@NpmPackage(value = "@polymer/paper-input", version = "3.0.2")
@NpmPackage(value = "@polymer/paper-checkbox", version = "3.0.1")
public class NpmPackageLayout extends Div implements RouterLayout {
}
