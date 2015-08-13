package com.vaadin.ui;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTest;

public class ComponentTestBase {

    protected static <T extends Component> T createComponent(Class<T> cls,
            String html) {
        try {
            T c = cls.newInstance();
            setComponentElement(c, html);
            return c;
        } catch (InstantiationException | IllegalAccessException e1) {
            throw new RuntimeException(e1);
        }
    }

    protected static void setComponentElement(Component c, String html) {
        Element e = ElementTest.parse(html);
        ((AbstractComponent) c).setElement(e);

    }

}
