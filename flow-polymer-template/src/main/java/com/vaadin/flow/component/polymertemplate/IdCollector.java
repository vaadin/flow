/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import org.jsoup.nodes.Element;

import com.vaadin.flow.component.template.Id;

/**
 * Collects information of {@link Id @Id} mapped fields in a template class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 * @deprecated Use
 *             {@link com.vaadin.flow.component.template.internal.IdCollector}
 *             instead. Polymer template support is deprecated - we recommend
 *             you to use {@code LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class IdCollector
        extends com.vaadin.flow.component.template.internal.IdCollector {

    /**
     * Creates a collector the the given template.
     *
     * @param templateClass
     *            the template class, containing the {@code @Id} fields
     * @param templateFile
     *            The name of the file containing the template or
     *            <code>null</code> if not available {@code null}
     * @param templateRoot
     *            The root element of the template or <code>null</code> if not
     *            available
     */
    public IdCollector(Class<?> templateClass, String templateFile,
            Element templateRoot) {
        super(templateClass, templateFile, templateRoot);
    }

}
