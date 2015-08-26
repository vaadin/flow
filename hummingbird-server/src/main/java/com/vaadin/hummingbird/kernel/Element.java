package com.vaadin.hummingbird.kernel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.annotations.EventParameter;
import com.vaadin.annotations.EventType;
import com.vaadin.event.EventListener;
import com.vaadin.shared.util.SharedUtil;
import com.vaadin.ui.Component;

import elemental.json.JsonObject;

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
        debug("Set attribute " + name + " for " + this);
        return this;
    }

    public static void debug(String string) {
        assert string != null;

        if (true) {
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

        template.setAttribute(name, value, node);
        return this;
    }

    public String getAttribute(String name) {
        assert validAttribute(name);

        Object value = template.getAttribute(name, node);
        return value == null ? null : value.toString();
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

    Collection<String> getEventData(String eventType) {
        assert eventType != null;

        return template.getEventData(eventType, node);
    }

    public Element addEventListener(String eventType,
            DomEventListener listener) {
        assert eventType != null;
        assert listener != null;

        template.addEventListener(eventType, listener, node);
        return this;
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
            return "#text: " + getTextNodeText(this);
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
     * Modifies the "class" attribute.
     * <p>
     * Has no effect if the element does not have the given class attribute
     *
     * @param className
     *            the class to remove
     * @return this element
     */
    public Element removeClass(String className) {
        assert className != null;

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
        assert className != null;

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
        assert className != null;

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
        getTextContent(this, b);
        return b.toString();
    }

    private void getTextContent(Element e, StringBuilder b) {
        assert e != null;
        assert b != null;

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

    private static boolean isTextNode(Element e) {
        return e != null && TEXT_NODE_TAG.equals(e.getTag());
    }

    /**
     * Focuses the element when it is attached to the DOM.
     */
    public void focus() {
        getNode().enqueueRpc("$0.focus()", this);
    }

    /**
     * Scrolls the element when it is attached to the DOM.
     */
    public void scrollIntoView() {
        getNode().enqueueRpc("$0.scrollIntoView()", this);
    }

    /**
     * Scrolls the element to the given top coordinate
     */
    public void setScrollTop(int scrollTop) {
        getNode().enqueueRpc("$0.scrollTop=$1", this, scrollTop);
    }

    /**
     * Scrolls the element to the given left coordinate
     */
    public void setScrollLeft(int scrollLeft) {
        getNode().enqueueRpc("$0.scrollLeft=$1", this, scrollLeft);
    }

    public <E extends EventObject> Element removeEventListener(
            Class<E> eventType, EventListener<E> listener, Object source) {
        assert eventType != null;
        assert listener != null;
        assert source != null;

        removeEventListener(getDomEventType(eventType),
                new DomEventListenerWrapper<E>(eventType, listener, source));

        return this;
    }

    public <E extends EventObject> Element addEventListener(Class<E> eventType,
            com.vaadin.event.EventListener<E> listener, Object source) {
        assert eventType != null;
        assert listener != null;
        assert source != null;

        if (eventType.getEnclosingClass() != null
                && !Modifier.isStatic(eventType.getModifiers())) {
            // Non static inner class
            throw new IllegalArgumentException(
                    "Event classes must be top level classes or static inner classes. "
                            + eventType.getName()
                            + " is a non-static inner class");
        }

        String domEventType = getDomEventType(eventType);
        addEventData(domEventType, eventType);
        addEventListener(domEventType,
                new DomEventListenerWrapper<E>(eventType, listener, source));

        return this;
    }

    private void addEventData(String domEventType,
            Class<? extends EventObject> eventType) {
        assert domEventType != null && !domEventType.isEmpty();
        assert eventType != null;

        for (Field f : getEventParameterFields(eventType)) {
            String eventParameter = getDomEventParameterName(f);
            addEventData(domEventType, eventParameter);
        }
    }

    private String getDomEventType(Class<? extends EventObject> eventType) {
        assert eventType != null;

        EventType ann = eventType.getAnnotation(EventType.class);
        if (ann == null) {
            throw new IllegalArgumentException(
                    "Event type " + eventType.getName() + " should have an @"
                            + EventType.class.getSimpleName() + " annotation");
        }
        return ann.value();
    }

    public static class DomEventListenerWrapper<E extends EventObject>
            implements DomEventListener {

        private EventListener<E> listener;
        private Object eventSource;
        private Class<E> eventType;

        public DomEventListenerWrapper(Class<E> eventType,
                EventListener<E> listener, Object eventSource) {
            assert eventType != null;
            assert listener != null;
            assert eventSource != null;

            this.eventType = eventType;
            this.listener = listener;
            this.eventSource = eventSource;
        }

        @Override
        public void handleEvent(JsonObject eventData) {
            assert eventData != null;

            E eventObject = createEventObject();

            populateEvent(eventObject, eventData);
            listener.onEvent(eventObject);
        }

        private E createEventObject() {
            for (Constructor<?> c : eventType.getConstructors()) {
                if (c.getParameterCount() == 1 && c.getParameterTypes()[0]
                        .isAssignableFrom(eventSource.getClass())) {
                    try {
                        return eventType.cast(c.newInstance(eventSource));
                    } catch (InstantiationException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException e) {
                        throw new RuntimeException(
                                "Unable to create event object instanceof of type "
                                        + eventType.getName(),
                                e);
                    }
                }
            }
            throw new RuntimeException(
                    "Unable to create event of type " + eventType.getName()
                            + ". No constructor accepting the event source of type "
                            + eventSource.getClass().getName() + " found.");
        }

        private void populateEvent(E eventObject, JsonObject eventData) {
            assert eventObject != null;
            assert eventData != null;

            for (Field f : getEventParameterFields(eventType)) {
                String value = getDomEventParameterName(f);

                f.setAccessible(true);
                try {
                    Object decodedValue = JsonConverter.fromJson(f.getType(),
                            eventData.get(value));
                    f.set(eventObject, decodedValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(
                            "Unable to assign value to field " + f.getName()
                                    + " in event object of type "
                                    + eventType.getName(),
                            e);
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DomEventListenerWrapper<?>)) {
                return false;
            }
            return listener.equals(((DomEventListenerWrapper<?>) obj).listener);
        }

        @Override
        public int hashCode() {
            return listener.hashCode() * 37 + eventSource.hashCode();
        }
    }

    private static String getDomEventParameterName(Field f) {
        assert f != null;

        EventParameter param = f.getAnnotation(EventParameter.class);
        assert param != null;
        String value = param.value();
        if (value.equals("")) {
            value = f.getName();
        }

        return value;
    }

    private static List<Field> getEventParameterFields(Class<?> eventType) {
        assert eventType != null;

        List<Field> fields = new ArrayList<>();
        // TODO Cache
        while (eventType != Object.class) {
            Arrays.stream(eventType.getDeclaredFields())
                    .filter(f -> f.getAnnotation(EventParameter.class) != null)
                    .forEach(fields::add);
            eventType = eventType.getSuperclass();
        }
        return fields;
    }

    public void dispatchEvent(String eventType, JsonObject eventData) {
        assert eventType != null;
        assert eventData != null;

        Collection<DomEventListener> listeners = getEventListeners(eventType);
        if (listeners.isEmpty()) {
            debug("No listeners for '" + eventType + "' event");

            return;
        }

        debug("Dispatching '" + eventType + "' event to " + listeners.size()
                + " listeners. Event data: " + eventData.toJson());
        for (DomEventListener listener : listeners
                .toArray(new DomEventListener[listeners.size()])) {
            listener.handleEvent(eventData);
        }

    }

}
