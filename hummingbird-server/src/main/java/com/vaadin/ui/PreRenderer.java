package com.vaadin.ui;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

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
                if (Element.isTextNode(childElement)
                        && (childElement.getAttribute("content") == null
                                || childElement.getTextContent().isEmpty())) {
                    continue;
                }

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

    /**
     * Converts the given pre-render tree to a JSoup tree
     *
     * @param document
     *            The JSoup document
     * @param preRenderTree
     *            The pre-render tree to convert
     * @return A JSoup node containing the converted pre-render tree
     */
    public static Node toJSoup(Document document,
            com.vaadin.hummingbird.kernel.Element preRenderTree) {
        if (com.vaadin.hummingbird.kernel.Element.isTextNode(preRenderTree)) {
            return new TextNode(preRenderTree.getTextContent(),
                    document.baseUri());
        } else {
            org.jsoup.nodes.Element target = document
                    .createElement(preRenderTree.getTag());
            preRenderTree.getAttributeNames().forEach(name -> {
                if (name.equals("innerHTML")) {
                    target.html(preRenderTree.getAttribute(name));
                } else {
                    target.attr(name, preRenderTree.getAttribute(name));
                }
            });
            int childCount = preRenderTree.getChildCount();
            for (int i = 0; i < childCount; i++) {
                target.appendChild(
                        toJSoup(document, preRenderTree.getChild(i)));
            }
            return target;
        }
    }

}
