package com.vaadin.guice.nonoverride;

import com.google.inject.AbstractModule;

import com.vaadin.guice.testClasses.AnImplementation;
import com.vaadin.guice.testClasses.AnInterface;
import com.vaadin.guice.testClasses.AnotherInterface;
import com.vaadin.guice.testClasses.AnotherInterfaceImplementation;

public class NonOverrideModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(AnImplementation.class);
        bind(AnotherInterface.class).to(AnotherInterfaceImplementation.class);
    }
}
