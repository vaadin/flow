package com.vaadin.flow.osgi;

import org.osgi.service.component.annotations.Component;

/**
 * 
 * Implementation of {@link OSGiMarker} : presence of {@link OSGiMarker} service
 * in {@link Lookup} means the app is executed in OSGi env because standard
 * Lookup doesn't know anything about {@link OSGiMarker}.
 */
@Component(immediate = true)
public class OSGiMarkerImpl implements OSGiMarker {

}
