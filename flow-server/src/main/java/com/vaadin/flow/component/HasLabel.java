package com.vaadin.flow.component;

/**
 * A component that supports label definition. 
 * <p>
 * The default implementations set the label of the component to the given text for
 * {@link #getElement()}. Override all methods in this interface if the text
 * should be added to some other element.
 *
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public interface HasLabel extends HasElement{

	 static final String LABEL_PROPERTY_NAME = "label";

	/**
     * Set the label of the component to the given text.
     * <p>
     * Any HTML is automatically escaped to prevent injection attacks.
     *
     * @param label
     *            the label text to set
     */
    default public void setLabel(String label) {
        getElement().setProperty(LABEL_PROPERTY_NAME, label == null ? "" : label);
    }

    /**
     * Gets the label of the component.
     *
     * @return the label of the component or <code>""</code> if no label has
     *         been set
     */
    default String getLabel() {
        return getElement().getProperty(LABEL_PROPERTY_NAME, "");
    }
}
