package com.vaadin.ui;

import java.util.List;

import com.vaadin.hummingbird.kernel.Element;

public class PreRenderer {

    public static Element preRenderElementTree(Element element) {
        Element preRenderedElement = cloneElementForPreRendering(element);

        for (int i = 0; i < element.getChildCount(); i++) {
            Element childElement = element.getChild(i);

            Element childPreRenderedElement;
            List<Component> components = childElement.getComponents();
            if (!components.isEmpty()) {
                // The last component in the component chain is the outermost
                // parent, let that handle pre-rendering. It will delegate to
                // the following components as needed
                Component lastComponent = components.get(components.size() - 1);
                childPreRenderedElement = lastComponent.preRender();
            } else {
                childPreRenderedElement = preRenderElementTree(childElement);
            }
            preRenderedElement.appendChild(childPreRenderedElement);
        }
        return preRenderedElement;
    }

    private static Element cloneElementForPreRendering(Element source) {
        Element target = new Element(source.getTag());
        for (String key : source.getAttributeNames()) {
            target.setAttribute(key, escapeAttribute(source.getAttribute(key)));
        }

        return target;
    }

    private static String escapeAttribute(String attribute) {
        // FIXME
        return attribute;
    }

}
