package com.vaadin.devbundle;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@JsModule("@polymer/paper-input/paper-input.js")
@JsModule("@polymer/paper-checkbox/paper-checkbox.js")
@NpmPackage(value = "@polymer/paper-input", version = "3.0.2")
@NpmPackage(value = "@polymer/paper-checkbox", version = "3.0.1")
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class Deps {

}
