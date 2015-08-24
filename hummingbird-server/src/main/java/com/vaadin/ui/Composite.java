package com.vaadin.ui;

import java.util.Collections;
import java.util.List;

import com.vaadin.hummingbird.kernel.Element;

public abstract class Composite extends AbstractComponent
        implements HasComponents {
    /*
     * Composite implementation is black magic. Consult with creator before
     * making changes...
     */

    private Component compositionRoot;

    public Composite() {
    }

    private Component getContent() {
        if (compositionRoot == null) {
            setContent(initContent());
        }

        return compositionRoot;
    }

    private void setContent(Component compositionRoot) {
        this.compositionRoot = compositionRoot;
        Element element = compositionRoot.getElement();
        element.getTemplate().getComponents(element.getNode(), true).add(this);
    }

    @Override
    public List<Component> getChildComponents() {
        return Collections.singletonList(getContent());
    }

    protected abstract Component initContent();

    @Override
    protected void setElement(Element element) {
        // Element is retrieved from composition root
        return;
    }

    @Override
    public Element getElement() {
        return getContent().getElement();
    }

    @Override
    public Component getParent() {
        return super.getParent();
    }
}
