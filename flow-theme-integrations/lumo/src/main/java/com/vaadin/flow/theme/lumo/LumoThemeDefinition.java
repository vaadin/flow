/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.theme.lumo;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.theme.ThemeDefinition;

/**
 * {@link Lumo} theme definition.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
@Component(service = ThemeDefinition.class)
public class LumoThemeDefinition extends ThemeDefinition {

    /**
     * Creates a new instance of {@link Lumo} theme definition.
     */
    public LumoThemeDefinition() {
        super(Lumo.class, "", "");
    }

}
