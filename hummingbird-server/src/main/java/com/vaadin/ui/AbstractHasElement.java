package com.vaadin.ui;

import java.util.logging.Logger;

import com.vaadin.annotations.Tag;
import com.vaadin.event.EventRouter;
import com.vaadin.event.HasEventRouter;
import com.vaadin.hummingbird.kernel.Element;

public class AbstractHasElement implements HasElement, HasEventRouter {

    private EventRouter eventRouter = null;
    private Element element;

    public AbstractHasElement() {
        Tag tag = getClass().getAnnotation(Tag.class);
        if (tag == null) {
            throw new IllegalStateException(
                    "No @Tag defined for " + getClass().getName());
        }
        if (tag.is().isEmpty()) {
            createElement(tag.value());
        } else {
            createElement(tag.value(), tag.is());
        }
    }

    protected AbstractHasElement(String tagName) {
        createElement(tagName);
    }

    private void createElement(String tagName) {
        setElement(new Element(tagName));
    }

    private void createElement(String tagName, String is) {
        setElement(new Element(tagName, is));
    }

    @Override
    public EventRouter getEventRouter() {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }

        return eventRouter;
    }

    @Override
    public Element getElement() {
        return element;
    }

    /**
     * Assigns the root element.
     *
     * @param element
     *            the element to use
     */
    protected void setElement(Element element) {
        if (element.getComponents().contains(this)) {
            throw new IllegalArgumentException(
                    "The same element is already set for this component");
        }
        this.element = element;
        if (!element.getComponents().isEmpty()) {
            if (element.getComponents().size() == 1
                    && element.getComponents().get(0) == this) {
                getLogger().warning("Element already set for this component");
                return;
            } else {
                throw new IllegalArgumentException(
                        "The given element is already attached to another component");
            }
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractHasElement.class.getName());
    }
}
