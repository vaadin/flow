/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.pro.licensechecker.LicenseChecker;

/**
 * Component for an HTML element declared as a polymer component. The HTML
 * markup should be loaded using the {@link JsModule} annotation and the
 * components should be associated with the web component element using the
 * {@link Tag @Tag} annotation.
 * <p>
 * You may use {@link Id} annotation inside your template class for a field to
 * reference an element inside your template via <b>id</b> attribute value. Note
 * that the injected element will have functional limitations on the server
 * side.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 *
 * @see JsModule
 * @see Tag
 * @see Id
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public abstract class PolymerTemplate<M extends TemplateModel>
        extends AbstractTemplate<M> implements Template {

    static {
        UsageStatistics.markAsUsed("flow/PolymerTemplate", null);

        LicenseChecker.checkLicenseFromStaticBlock("flow-polymer-template",
                Version.getFullVersion(), null);
    }

    /**
     * Creates the component that is responsible for Polymer template
     * functionality using the provided {@code parser}.
     *
     * @param parser
     *            a template parser
     */
    public PolymerTemplate(TemplateParser parser) {
        this(parser, VaadinService.getCurrent());
    }

    /**
     * Creates the component that is responsible for Polymer template
     * functionality using the provided {@code parser}.
     *
     * @param parser
     *            a template parser
     * @param service
     *            the related service instance
     */
    protected PolymerTemplate(TemplateParser parser, VaadinService service) {
        if (service == null) {
            throw new IllegalStateException(VaadinService.class.getSimpleName()
                    + " instance is null. "
                    + "It means that you are trying to create "
                    + "a component instance outside of servlet request thread "
                    + "which is not thread safe. Any component "
                    + "instantiation logic should be protected by a session lock."
                    + "Call your logic inside the UI::access method.");
        }
        TemplateInitializer templateInitializer = new TemplateInitializer(this,
                parser, service);
        templateInitializer.initChildElements();

        Set<String> twoWayBindingPaths = templateInitializer
                .getTwoWayBindingPaths();

        initModel(twoWayBindingPaths);
    }

    /**
     * Creates the component that is responsible for Polymer template
     * functionality.
     */
    public PolymerTemplate() {
        this(VaadinService.getCurrent().getInstantiator()
                .getOrCreate(TemplateParser.TemplateParserFactory.class)
                .createParser(), VaadinService.getCurrent());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Please note that components defined using {@link Id @Id} are not child
     * components since they are attached inside the Shadow DOM. Only components
     * explicitly added through methods such as {@link HasComponents#add} or
     * {@link Element#appendChild(Element...)} are returned by this method.
     */
    @Override
    public Stream<Component> getChildren() {
        return super.getChildren();
    }

}
