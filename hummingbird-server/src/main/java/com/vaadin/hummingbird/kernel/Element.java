package com.vaadin.hummingbird.kernel;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.shared.util.SharedUtil;
import com.vaadin.ui.Component;

public class Element {

    private static final String TEXT_NODE_TEXT_ATTRIBUTE = "content";
    private static final String STYLE_SEPARATOR = ";";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String STYLE_ATTRIBUTE = "style";
    private static final String TEXT_NODE_TAG = "#text";
    private ElementTemplate template;
    private StateNode node;

    public Element(String tag) {
        this(BasicElementTemplate.get(),
                BasicElementTemplate.createBasicElementModel(tag));
    }

    private static Logger getLogger() {
        return Logger.getLogger(Element.class.getName());
    }

    private Element(ElementTemplate template, StateNode node) {
        // Private constructor to force using the static getter that might
        // enable caching at some point
        if (!template.supports(node)) {
            getLogger().warning(
                    "Template " + template + " does not support node " + node);
        }
        this.template = template;
        this.node = node;
    }

    public String getTag() {
        return template.getTag(node);
    }

    public ElementTemplate getTemplate() {
        return template;
    }

    public StateNode getNode() {
        return node;
    }

    public Element setAttribute(String name, String value) {
        assert validAttribute(name);
        template.setAttribute(name, value, node);
        return this;
    }

    public Element setAttribute(String name, int value) {
        return setAttribute(name, String.valueOf(value));
    }

    private boolean validAttribute(String name) {
        if (TEXT_NODE_TAG.equals(getTag())) {
            assert TEXT_NODE_TEXT_ATTRIBUTE.equals(name) : "Attribute " + name
                    + " is not supported for text nodes";
        }
        return true;
    }

    public Element setAttribute(String name, boolean value) {
        assert validAttribute(name);
        template.setAttribute(name, Boolean.toString(value), node);
        return this;
    }

    public String getAttribute(String name) {
        return template.getAttribute(name, node);
    }

    public String getAttribute(String name, String defaultValue) {
        if (!hasAttribute(name)) {
            return defaultValue;
        }

        return getAttribute(name);
    }

    public int getAttribute(String name, int defaultValue) {
        if (!hasAttribute(name)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(getAttribute(name));
        } catch (Exception e) {
            getLogger().fine("Could not parse attribute value '" + defaultValue
                    + "' as integer");
            return defaultValue;
        }
    }

    public boolean hasAttribute(String name) {
        return getAttribute(name) != null;
    }

    public Element addEventListener(String type, EventListener listener) {
        template.addListener(type, listener, node);
        return this;
    }

    public Element removeEventListener(String type, EventListener listener) {
        template.removeListener(type, listener, node);
        return this;
    }

    public int getChildCount() {
        return template.getChildCount(node);
    }

    public Element getChild(int index) {
        return template.getChild(index, node);
    }

    public Element insertChild(int index, Element child) {
        assert child != null : "Cannot insert null child";
        template.insertChild(index, child, node);
        return this;
    }

    public Element appendChild(Element child) {
        insertChild(getChildCount(), child);
        return this;
    }

    /**
     * Removes the element from its parent.
     * <p>
     * Fires a detach event when the element is removed.
     * <p>
     * Has not effect if the element does not have a parent.
     *
     * @return this element
     */
    public Element removeFromParent() {
        Element parent = getParent();
        if (parent != null) {
            parent.template.removeChild(parent.getNode(), this);
        }
        return this;
    }

    public Element getParent() {
        return template.getParent(node);
    }

    public static Element getElement(ElementTemplate template, StateNode node) {
        return new Element(template, node);
    }

    @Override
    public String toString() {
        return getOuterHTML();
    }

    public Collection<String> getAttributeNames() {
        return template.getAttributeNames(node);
    }

    public String getOuterHTML() {
        StringBuilder b = new StringBuilder();
        getOuterHTML(b);
        return b.toString();
    }

    private void getOuterHTML(StringBuilder b) {
        String tag = getTag();
        if (TEXT_NODE_TAG.equals(tag)) {
            String content = getTextNodeText(this);
            if (content != null) {
                b.append(content);
            }
        } else {
            b.append('<');
            b.append(tag);
            for (String attribute : getAttributeNames()) {
                String value = getAttribute(attribute);
                if (value != null) {
                    b.append(' ');
                    b.append(attribute);
                    b.append("=\"");
                    b.append(value);
                    b.append('\"');
                }
            }
            b.append('>');

            for (int i = 0; i < getChildCount(); i++) {
                getChild(i).getOuterHTML(b);
            }
            b.append("</");
            b.append(tag);
            b.append('>');
        }
    }

    private static Element createText(String content) {
        Element element = new Element(TEXT_NODE_TAG);
        element.setAttribute(TEXT_NODE_TEXT_ATTRIBUTE, content);
        return element;
    }

    public Element removeAllChildren() {
        while (getChildCount() > 0) {
            getChild(0).removeFromParent();
        }

        return this;
    }

    /**
     * Adds the given attribute value to the given attribute, which consists of
     * a list of values separated by the given separator
     * <p>
     * Has no effect is the attribute already contains the given value
     *
     * @param name
     *            The attribute name
     * @param valueToAdd
     *            the value to add to the attribute
     * @param separator
     *            the separator used between the attribute values
     * @return this element
     */
    protected Element addAttributeValue(String name, String valueToAdd,
            String separator) {
        if (!hasAttribute(name)) {
            return setAttribute(name, valueToAdd);
        } else {
            if (hasAttributeValue(name, valueToAdd, separator)) {
                // Already has the given attribute
                return this;
            }

            return setAttribute(name,
                    getAttribute(name) + separator + valueToAdd);
        }
    }

    /**
     * Checks the given attribute value exists in the given attribute, which
     * consists of a list of values separated by the given separator
     *
     * @param name
     *            The attribute name
     * @param valueToAdd
     *            the value to check for in the attribute
     * @param separator
     *            the separator used between the attribute values
     * @return true if the value exists in the attribute, false otherwise
     */
    protected boolean hasAttributeValue(String name, String valueToAdd,
            String separator) {
        if (hasAttribute(name)) {
            String[] currentValues = getAttribute(name).split(separator);
            for (String s : currentValues) {
                if (s.equals(valueToAdd)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the given attribute value from the given attribute, which
     * consists of a list of values separated by the given separator
     * <p>
     * Has no effect is the attribute does not contain the given value
     *
     * @param name
     *            The attribute name
     * @param valueToRemove
     *            the value to remove from the attribute
     * @param separator
     *            the separator used between the attribute values
     * @return this element
     */
    protected Element removeAttributeValue(String name, String valueToRemove,
            String separator) {
        if (!hasAttribute(name)) {
            return this;
        }

        String newValue = Arrays.stream(getAttribute(name).split(separator))
                .filter(value -> {
                    return !value.equals(valueToRemove);
                }).collect(Collectors.joining(separator));
        if (newValue.equals("")) {
            newValue = null;
        }

        return setAttribute(name, newValue);
    }

    public boolean hasChild(Element element) {
        return equals(element.getParent());
    }

    public int getChildIndex(Element element) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChild(i).equals(element)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Element)) {
            return false;
        }

        Element other = (Element) obj;

        return node.equals(other.node) && template.equals(other.template);
    }

    @Override
    public int hashCode() {
        return node.hashCode() + 31 * template.hashCode();
    }

    public Element removeAttribute(String name) {
        setAttribute(name, null);
        return this;
    }

    /**
     * Removes the given class from the element.
     * <p>
     * Modifies the "class" attribute.
     * <p>
     * Has no effect if the element does not have the given class attribute
     *
     * @param className
     *            the class to remove
     * @return this element
     */
    public Element removeClass(String className) {
        return removeAttributeValue(CLASS_ATTRIBUTE, className, " ");
    }

    /**
     * Adds the given class to the element.
     * <p>
     * Modifies the "class" attribute.
     * <p>
     * Has no effect if the element already has the given class name
     *
     * @param className
     *            the class name to add
     * @return this element
     */
    public Element addClass(String className) {
        if (!hasAttribute(CLASS_ATTRIBUTE)) {
            setAttribute(CLASS_ATTRIBUTE, className);
        } else {
            addAttributeValue(CLASS_ATTRIBUTE, className, " ");
        }
        return this;
    }

    /**
     * Checks if the element has the given class
     *
     * @param className
     *            the class name to check for
     * @return true if the class name is set, false otherwise
     */
    public boolean hasClass(String className) {
        return hasAttributeValue(CLASS_ATTRIBUTE, className, " ");
    }

    /**
     * Sets the given inline style ({@code property}: {@code value}) on the
     * element. Replaces any existing value with the same property.
     *
     * @param property
     *            The style property to set
     * @param value
     *            The value to set for the property
     */
    public Element setStyle(String property, String value) {
        if (!hasAttribute(STYLE_ATTRIBUTE)) {
            setAttribute(STYLE_ATTRIBUTE, property + ":" + value);
            return this;
        }
        String[] currentStyles = getAttribute(STYLE_ATTRIBUTE)
                .split(STYLE_SEPARATOR);

        boolean updated = false;
        for (int i = 0; i < currentStyles.length; i++) {
            String currentStyle = currentStyles[i];
            String[] keyValue = currentStyle.split(":", 2);
            String key = keyValue[0].trim();

            if (key.equals(property)) {
                updated = true;
                currentStyles[i] = property + ":" + value;
                break;
            }
        }

        String newStyles = SharedUtil.join(currentStyles, STYLE_SEPARATOR);
        if (!updated) {
            newStyles += STYLE_SEPARATOR + property + ":" + value;
        }
        setAttribute(STYLE_ATTRIBUTE, newStyles);
        return this;
    }

    /**
     * Checks if the the given inline style property has been defined for the
     * element
     *
     * @param property
     *            the style property to look for
     * @return true if the property has been set, false otherwise
     */
    public boolean hasStyle(String property) {
        return getStyle(property) != null;
    }

    /**
     * Gets the value of the given inline style property
     *
     * @param property
     *            The property whose value should be returned
     * @return The value of the property or null if the property has not been
     *         set
     */
    public String getStyle(String property) {
        if (!hasAttribute(STYLE_ATTRIBUTE)) {
            return null;
        }

        for (String style : getAttribute(STYLE_ATTRIBUTE)
                .split(STYLE_SEPARATOR)) {
            String[] keyValue = style.split(":", 2);
            String key = keyValue[0];
            if (key.equals(property)) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * Removes the given inline style property from the element.
     * <p>
     * Note that you should only pass the style property (e.g. width) and not
     * the value
     *
     * @param property
     *            the style property to remove
     */
    public Element removeStyle(String property) {
        if (!hasAttribute(STYLE_ATTRIBUTE)) {
            return this;
        }

        String newStyles = Arrays
                .stream(getAttribute(STYLE_ATTRIBUTE).split(STYLE_SEPARATOR))
                .filter(style -> {
                    String[] keyValue = style.split(":", 2);
                    String key = keyValue[0];
                    return !key.equals(property);
                }).collect(Collectors.joining(STYLE_SEPARATOR));

        if (newStyles.equals("")) {
            newStyles = null;
        }
        setAttribute(STYLE_ATTRIBUTE, newStyles);
        return this;
    }

    public Element setComponent(Component component) {
        template.setComponent(component, node);
        return this;
    }

    public Component getComponent() {
        return template.getComponent(node);
    }

    /**
     * Adds or removes the given class, based on the {@code add} parameter
     *
     * @param className
     *            the class to add or remove
     * @param enabled
     *            true to add the class, false to remove it
     */
    public Element setClass(String className, boolean add) {
        if (add) {
            return addClass(className);
        } else {
            return removeClass(className);
        }
    }

    /**
     * Removes all children from the element and sets the given text as its text
     * content
     *
     * @param text
     *            The text to set
     */
    public Element setTextContent(String text) {
        removeAllChildren();
        appendChild(createText(text));
        return this;
    }

    /**
     * Returns the text content of this element and all child elements
     * recursively
     *
     * @return
     */
    public String getTextContent() {
        StringBuilder b = new StringBuilder();
        getTextContent(this, b);
        return b.toString();
    }

    private void getTextContent(Element e, StringBuilder b) {

        for (int i = 0; i < e.getChildCount(); i++) {
            Element child = e.getChild(i);
            if (isTextNode(child)) {
                b.append(getTextNodeText(child));
            } else {
                getTextContent(child, b);
            }
        }
    }

    private String getTextNodeText(Element e) {
        assert isTextNode(e);
        assert e.hasAttribute(TEXT_NODE_TEXT_ATTRIBUTE);
        return e.getAttribute(TEXT_NODE_TEXT_ATTRIBUTE);
    }

    private boolean isTextNode(Element e) {
        return TEXT_NODE_TAG.equals(e.getTag());
    }

}
