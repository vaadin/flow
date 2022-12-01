/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.template.Id;

/**
 * Creates or maps Element instances to fields mapped using {@link Id @Id}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 * @deprecated Use {@link com.vaadin.flow.component.template.internal.IdMapper}
 *             instead.Polymer template support is deprecated - we recommend you
 *             to use {@code LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class IdMapper
        extends com.vaadin.flow.component.template.internal.IdMapper {

    /**
     * Creates a mapper for the given template.
     *
     * @param template
     *            a template instance
     */
    public IdMapper(Component template) {
        super(template);
    }

}
