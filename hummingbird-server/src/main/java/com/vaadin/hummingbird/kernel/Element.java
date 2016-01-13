package com.vaadin.hummingbird.kernel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.shared.util.SharedUtil;
import com.vaadin.ui.Component;

import elemental.json.JsonObject;

public class Element implements Serializable {

    public static final String TEXT_NODE_TEXT_ATTRIBUTE = "content";
    private static final String STYLE_SEPARATOR = ";";
    private static final String STYLE_ATTRIBUTE = "style";
    private static final String TEXT_NODE_TAG = "#text";
    private ElementTemplate template;
    private StateNode node;

    public Element(String tag) {
        this(BasicElementTemplate.get(),
                BasicElementTemplate.createBasicElementModel(tag));
    }

    public Element(String tag, String is) {
        this(BasicElementTemplate.get(),
                BasicElementTemplate.createBasicElementModel(tag, is));
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

    public String getIs() {
        return template.getIs(node);
    }

    public ElementTemplate getTemplate() {
        return template;
    }

    public StateNode getNode() {
        return node;
    }

    public Element setAttribute(String name, String value) {
        assert validAttribute(name);

        if (name.equals("class")) {
            throw new UnsupportedOperationException(
                    "Can't set element class with setAttribute, use setClass or addClass.");
        }

        if (name.equals("is")) {
            throw new UnsupportedOperationException(
                    "Can't set element's is-attribute with setAttribute, is must be set with @Is(\"\"");
        }

        template.setAttribute(name, value, node);
        debug("Set attribute " + name + " for " + this);
        return this;
    }

    public static void debug(String string) {
        assert string != null;

        if (false) {
            getLogger().info(string);
        }

    }

    public Element setAttribute(String name, int value) {
        assert validAttribute(name);

        template.setAttribute(name, value, node);
        debug("Set attribute " + name + " for " + this);
        return this;
    }

    public Element setAttribute(String name, double value) {
        assert validAttribute(name);

        template.setAttribute(name, value, node);
        debug("Set attribute " + name + " for " + this);
        return this;

    }

    private boolean validAttribute(String name) {
        if (name == null) {
            return false;
        }

        if (TEXT_NODE_TAG.equals(getTag())) {
            assert TEXT_NODE_TEXT_ATTRIBUTE.equals(name) : "Attribute " + name
                    + " is not supported for text nodes";
        }
        return true;
    }

    public Element setAttribute(String name, boolean value) {
        assert validAttribute(name);

        if (value) {
            template.setAttribute(name, Boolean.TRUE, node);
        } else {
            template.setAttribute(name, null, node);
        }
        return this;
    }

    public Object getRawAttribute(String name) {
        assert validAttribute(name);

        if (name.equals("class")) {
            return getClassNames();
        }

        if (name.equals("is")) {
            return getIs();
        }

        return template.getAttribute(name, node);
    }

    private static String formatAttributeValue(Object value) {
        // Integer-ish floating point values are shown as integers in
        // JS, but as e.g. 1.0 in Java.
        if (value instanceof Double || value instanceof Float) {
            Number number = (Number) value;
            long longValue = number.longValue();
            if (longValue == number.doubleValue()) {
                return Long.toString(longValue);
            }
        } else if (value instanceof Boolean) {
            if (Boolean.TRUE.equals(value)) {
                return "";
            } else {
                return null;
            }
        }
        return value == null ? null : value.toString();
    }

    public String getAttribute(String name) {
        return formatAttributeValue(getRawAttribute(name));
    }

    public String getAttribute(String name, String defaultValue) {
        assert validAttribute(name);

        if (!hasAttribute(name)) {
            return defaultValue;
        }

        return getAttribute(name);
    }

    public int getAttribute(String name, int defaultValue) {
        assert validAttribute(name);

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

    public double getAttribute(String name, double defaultValue) {
        assert validAttribute(name);

        if (!hasAttribute(name)) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(getAttribute(name));
        } catch (Exception e) {
            getLogger().fine("Could not parse attribute value '" + defaultValue
                    + "' as double");
            return defaultValue;
        }
    }

    public boolean hasAttribute(String name) {
        assert validAttribute(name);

        return getAttribute(name) != null;
    }

    public Element addEventData(String eventType, String... data) {
        assert eventType != null;

        for (String d : data) {
            debug("Add event data for " + eventType + ": " + d);
        }
        template.addEventData(eventType, node, data);
        return this;
    }

    public Element removeEventData(String eventType, String... data) {
        assert eventType != null;

        for (String d : data) {
            debug("Removing event data for " + eventType + ": " + d);
        }
        template.removeEventData(eventType, node, data);
        return this;
    }

    Collection<String> getEventData(String eventType) {
        assert eventType != null;

        return template.getEventData(eventType, node);
    }

    public interface EventRegistrationHandle {
        public void remove();
    }

    /**
     * Adds an event listener for the given event type
     *
     * @param eventType
     *            the type of event to listen to
     * @param listener
     *            the listener to add
     * @return a handle which can be used for removing the listener
     */
    public EventRegistrationHandle addEventListener(String eventType,
            DomEventListener listener) {
        assert eventType != null : "Event type must not be null";
        assert listener != null : "Listener must not be null";

        template.addEventListener(eventType, listener, node);
        return () -> Element.this.removeEventListener(eventType, listener);
    }

    public Element removeEventListener(String eventType,
            DomEventListener listener) {
        assert eventType != null;
        assert listener != null;

        template.removeEventListener(eventType, listener, node);
        return this;
    }

    public boolean hasEventListeners(String eventType) {
        assert eventType != null;
        return !template.getEventListeners(eventType, node).isEmpty();
    }

    public Collection<DomEventListener> getEventListeners(String eventType) {
        assert eventType != null;

        return template.getEventListeners(eventType, node);
    }

    public int getChildCount() {
        return template.getChildCount(node);
    }

    public Element getChild(int index) {
        assert index >= 0;

        return template.getChild(index, node);
    }

    public Element insertChild(int index, Element child) {
        assert index >= 0;
        assert child != null : "Cannot insert null child";

        template.insertChild(index, child, node);
        assert child.getParent().equals(
                this) : "Child should have this as parent after being inserted";

        return this;
    }

    public Element setChild(int index, Element child) {
        assert index >= 0;
        assert child != null;

        int childCount = getChildCount();
        if (index < childCount) {
            removeChild(index);
            insertChild(index, child);
        } else if (index == getChildCount()) {
            insertChild(childCount, child);
        } else {
            throw new IllegalArgumentException("Cannot set child element "
                    + index + " when there are only " + childCount
                    + " child elements");
        }
        return this;
    }

    public Element appendChild(Element child) {
        assert child != null : "Cannot insert null child";

        insertChild(getChildCount(), child);
        assert child.getParent().equals(this);

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
        assert template != null;
        assert node != null;

        return new Element(template, node);
    }

    @Override
    public String toString() {
        if (isTextNode(this)) {
            return "#text: " + getTextNodeText();
        }
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
            String content = getTextNodeText();
            if (content != null) {
                b.append(content);
            }
        } else {
            b.append('<');
            b.append(tag);
            if (getIs() != null) {
                b.append(" is=\"").append(getIs()).append("\"");
            }
            for (String attribute : getAttributeNames()) {
                if (attribute.equals("innerHTML")) {
                    continue;
                }
                if (attribute.equals("class")) {
                    String classNames = getClassNames();
                    if (!classNames.isEmpty()) {
                        b.append(" class=\"");
                        b.append(classNames);
                        b.append("\"");
                    }
                } else {
                    Object rawValue = getRawAttribute(attribute);
                    if (rawValue != null && !Boolean.FALSE.equals(rawValue)) {
                        b.append(' ');
                        b.append(attribute);
                        if (!rawValue.equals(Boolean.TRUE)) {
                            String value = formatAttributeValue(rawValue);
                            b.append("=\"");
                            b.append(value);
                            b.append('\"');
                        }
                    }
                }
            }
            b.append('>');

            if (hasAttribute("innerHTML")) {
                b.append(getAttribute("innerHTML"));
            }

            for (int i = 0; i < getChildCount(); i++) {
                getChild(i).getOuterHTML(b);
            }
            b.append("</");
            b.append(tag);
            b.append('>');
        }
    }

    public static Element createText(String content) {
        assert content != null;

        Element element = new Element(TEXT_NODE_TAG);
        element.setAttribute(TEXT_NODE_TEXT_ATTRIBUTE, content);
        return element;
    }

    public Element removeChild(Element element) {
        assert element != null;

        if (!element.getParent().equals(this)) {
            throw new IllegalArgumentException(
                    "The given element is not a child of this element");
        }
        template.removeChild(getNode(), element);
        return this;
    }

    public Element removeChild(int index) {
        assert index >= 0;
        assert index < getChildCount();

        Element element = getChild(index);
        assert element.getParent().equals(this);

        template.removeChild(getNode(), element);
        return this;
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
        assert validAttribute(name);
        assert valueToAdd != null;
        assert separator != null;

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
        assert validAttribute(name);
        assert valueToAdd != null;
        assert separator != null;

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
        assert validAttribute(name);
        assert valueToRemove != null;
        assert separator != null;

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
        assert element != null;

        return equals(element.getParent());
    }

    public int getChildIndex(Element element) {
        assert element != null;
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
        assert validAttribute(name);

        setAttribute(name, null);
        return this;
    }

    /**
     * Removes the given class from the element.
     * <p>
     * Modifies the "classList" attribute.
     * <p>
     * Has no effect if the element does not have the given class attribute.
     * <p>
     * Does not remove classes bound by the element template.
     *
     * @param className
     *            the class to remove
     * @return this element
     */
    public Element removeClass(String className) {
        assert className != null;

        template.getClassList(node, false).remove(className);
        return this;
    }

    /**
     * Adds the given class to the element.
     * <p>
     * Modifies the "classList" property. Multiple class names can be by
     * separating the class names with whitespace.
     * <p>
     * Has no effect if the element already has the given class name.
     * <p>
     * Empty string has not effect.
     *
     * @param className
     *            the class name to add
     * @return this element
     */
    public Element addClass(String className) {
        assert className != null;
        if (className.isEmpty()) {
            return this;
        }
        if (className.contains(" ")) {
            // Split space separated style names and add them one by one.
            StringTokenizer tokenizer = new StringTokenizer(className, " ");
            while (tokenizer.hasMoreTokens()) {
                addClass(tokenizer.nextToken());
            }

        } else {
            List<String> classList = template.getClassList(node, true);

            if (!classList.contains(className)) {
                classList.add(className);
            }
        }

        return this;
    }

    /**
     * Checks if the element has the given class.
     *
     * @param className
     *            the class name to check for
     * @return true if the class name is set, false otherwise
     */
    public boolean hasClass(String className) {
        assert className != null;

        return getClasses().contains(className);
    }

    /**
     * Returns a list of the current classes for this element, if any.
     *
     * @return the class list
     */
    public List<String> getClasses() {
        return template.getAllClasses(node);
    }

    /**
     * Return the current classes for this element, or empty string if none
     * exist.
     *
     * @return all the set classes for this element
     */
    public String getClassNames() {
        final StringBuilder sb = new StringBuilder();
        getClasses().forEach(str -> {
            sb.append(str);
            sb.append(" ");
        });
        return sb.toString().trim();
    }

    /**
     * Removes all classes for this element, if any.
     */
    public Element removeAllClasses() {
        template.getClassList(node, false).clear();
        return this;
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
    public Element setStyle(String property, int value) {
        return setStyle(property, value + "");

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
        assert property != null;
        assert value != null;

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
        assert property != null;

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
        assert property != null;

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
        assert property != null;

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

    public List<Component> getComponents() {
        return template.getComponents(node, false);
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
        assert className != null;

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
        if (text != null) {
            appendChild(createText(text));
        }
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
        getTextContent(b);
        return b.toString();
    }

    private void getTextContent(StringBuilder b) {
        assert b != null;
        if (isTextNode(this)) {
            b.append(getTextNodeText());
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                Element child = getChild(i);
                child.getTextContent(b);
            }
        }
    }

    private String getTextNodeText() {
        assert isTextNode(this);
        assert hasAttribute(TEXT_NODE_TEXT_ATTRIBUTE);
        return getAttribute(TEXT_NODE_TEXT_ATTRIBUTE);
    }

    public static boolean isTextNode(Element e) {
        return e != null && TEXT_NODE_TAG.equals(e.getTag());
    }

    /**
     * Focuses the element when it is attached to the DOM.
     */
    public Element focus() {
        invoke("focus");
        return this;
    }

    /**
     * Scrolls the element when it is attached to the DOM.
     */
    public Element scrollIntoView() {
        invoke("scrollIntoView");
        return this;
    }

    /**
     * Scrolls the element to the given top coordinate
     */
    public Element setScrollTop(int scrollTop) {
        getNode().enqueueRpc("$0.scrollTop=$1", this, scrollTop);
        return this;
    }

    /**
     * Scrolls the element to the given left coordinate
     */
    public Element setScrollLeft(int scrollLeft) {
        getNode().enqueueRpc("$0.scrollLeft=$1", this, scrollLeft);
        return this;
    }

    public Element dispatchEvent(String eventType, JsonObject eventData) {
        assert eventType != null;
        assert eventData != null;

        Collection<DomEventListener> listeners = getEventListeners(eventType);
        if (listeners.isEmpty()) {
            debug("No listeners for '" + eventType + "' event");

            return this;
        }

        debug("Dispatching '" + eventType + "' event to " + listeners.size()
                + " listeners. Event data: " + eventData.toJson());
        for (DomEventListener listener : listeners
                .toArray(new DomEventListener[listeners.size()])) {
            listener.handleEvent(eventData);
        }

        return this;
    }

    public Element runBeforeNextClientResponse(Runnable runnable) {
        template.runBeforeNextClientResponse(runnable, getNode());
        return this;
    }

    public StateNode getElementDataNode() {
        return template.getElementDataNode(node, true);
    }

    public Element invoke(String methodName, Object... arguments) {
        String paramString = IntStream.range(1, arguments.length + 1)
                .mapToObj(i -> ("$" + i)).collect(Collectors.joining(","));

        Object[] rpcArgs = new Object[arguments.length + 1];
        rpcArgs[0] = this;
        for (int i = 0; i < arguments.length; i++) {
            rpcArgs[i + 1] = arguments[i];
        }
        getNode().enqueueRpc("$0." + methodName + "(" + paramString + ")",
                rpcArgs);

        return this;
    }

}
