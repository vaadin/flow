/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.di.Lookup;

/**
 *
 * Implementation of {@link OSGiMarker} : presence of {@link OSGiMarker} service
 * in {@link Lookup} means the app is executed in OSGi env because standard
 * Lookup doesn't know anything about {@link OSGiMarker}.
 */
@Component(immediate = true)
public class OSGiMarkerImpl implements OSGiMarker {

}
