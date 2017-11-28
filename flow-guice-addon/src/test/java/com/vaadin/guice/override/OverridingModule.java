package com.vaadin.guice.override;

import com.google.inject.AbstractModule;

import com.vaadin.guice.annotation.OverrideBindings;
import com.vaadin.guice.testClasses.ASecondImplementation;
import com.vaadin.guice.testClasses.AnInterface;

@OverrideBindings
public class OverridingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AnInterface.class).to(ASecondImplementation.class);
    }
}
