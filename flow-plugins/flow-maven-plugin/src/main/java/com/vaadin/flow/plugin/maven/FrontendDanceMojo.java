/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * This is the hidden `vaadin:dance` to clean up the frontend files.
 *
 * @since
 */
@Mojo(name = "dance", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class FrontendDanceMojo extends CleanFrontendMojo {
}
