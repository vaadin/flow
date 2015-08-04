package com.vaadin.tests.components;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * 
 * @deprecated Use {@link AbstractTestUI} or {@link AbstractTestUIWithLog}
 *             instead. TestBase is a LegacyApplication
 */
@Deprecated
public abstract class TestBase extends AbstractTestCase {

    @Override
    public final void init(VaadinRequest request) {
        VerticalLayout vl = new VerticalLayout();
        setContent(vl);
        vl.setSizeFull();

        Label label = new Label(getTestDescription(), ContentMode.HTML);
        if (label.getValue() == null || "".equals(label.getValue())) {
            // This is only an ugly hack to be screenshot compatible to be able
            // to detect real problems when introducing IE font-size/line-height
            // fixes
            label.setValue("&nbsp;");
            if (getBrowser().isIE() && getBrowser().getBrowserMajorVersion() == 9) {
                label.setHeight("13.8px");
            } else {
                label.setHeight("15px");
            }
        }

        label.setWidth("100%");
        vl.addComponent(label);
        vl.addComponent(layout);
        vl.setExpandRatio(layout, 1);

        setup();
    }

    private VerticalLayout layout = new VerticalLayout();

    public TestBase() {

    }

    protected VerticalLayout getLayout() {
        return layout;
    }

    protected abstract void setup();

    protected void addComponent(Component c) {
        getLayout().addComponent(c);
    }

    protected void removeComponent(Component c) {
        getLayout().removeComponent(c);
    }

    protected void replaceComponent(Component oldComponent, Component newComponent) {
        getLayout().replaceComponent(oldComponent, newComponent);
    }

}
