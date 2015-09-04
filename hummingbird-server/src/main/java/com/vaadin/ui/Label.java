/*
 * Copyright 2000-2014 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.ui;

import java.util.Locale;
import java.util.Objects;

import com.vaadin.annotations.Tag;
import com.vaadin.data.Property;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.ConverterUtil;
import com.vaadin.shared.ui.label.ContentMode;

/**
 * Label component for showing non-editable short texts.
 *
 * The label content can be set to the modes specified by {@link ContentMode}
 *
 * <p>
 * The contents of the label may contain simple formatting:
 * <ul>
 * <li><b>&lt;b></b> Bold
 * <li><b>&lt;i></b> Italic
 * <li><b>&lt;u></b> Underlined
 * <li><b>&lt;br/></b> Linebreak
 * <li><b>&lt;ul>&lt;li>item 1&lt;/li>&lt;li>item 2&lt;/li>&lt;/ul></b> List of
 * items
 * </ul>
 * The <b>b</b>,<b>i</b>,<b>u</b> and <b>li</b> tags can contain all the tags in
 * the list recursively.
 * </p>
 *
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings("serial")
@Tag("div")
public class Label extends Composite implements Property<String>,
        Property.Viewer, Property.ValueChangeListener,
        Property.ValueChangeNotifier, Comparable<Label> {

    /**
     * A converter used to convert from the data model type to the field type
     * and vice versa. Label type is always String.
     */
    private Converter<String, Object> converter = null;

    private Property<String> dataSource = null;

    private ContentMode contentMode = ContentMode.TEXT;

    /**
     * Creates an empty Label.
     */
    public Label() {
        this("");
    }

    /**
     * Creates a new instance of Label with text-contents.
     *
     * @param content
     */
    public Label(String content) {
        this(content, ContentMode.TEXT);
    }

    /**
     * Creates a new instance of Label with text-contents read from given
     * datasource.
     *
     * @param contentSource
     */
    public Label(Property contentSource) {
        this(contentSource, ContentMode.TEXT);
    }

    /**
     * Creates a new instance of Label with text-contents.
     *
     * @param content
     * @param contentMode
     */
    public Label(String content, ContentMode contentMode) {
        super();
        setValue(content);
        setContentMode(contentMode);
    }

    /**
     * Creates a new instance of Label with text-contents read from given
     * datasource.
     *
     * @param contentSource
     * @param contentMode
     */
    public Label(Property contentSource, ContentMode contentMode) {
        setPropertyDataSource(contentSource);
        setContentMode(contentMode);
    }

    /**
     * Gets the value of the label.
     * <p>
     * The value of the label is the text that is shown to the end user.
     * Depending on the {@link ContentMode} it is plain text or markup.
     * </p>
     *
     * @return the value of the label.
     */
    @Override
    public String getValue() {
        if (getPropertyDataSource() == null) {
            // Use internal value if we are running without a data source
            return getDomValue();
        }
        return getDataSourceValue();
    }

    private String getDomValue() {
        if (getContentMode() == ContentMode.TEXT) {
            return getContent().getElement().getTextContent();
        } else if (getContentMode() == ContentMode.HTML) {
            return getContent().getInnerHtml();
        } else if (getContentMode() == ContentMode.PREFORMATTED) {
            // "<pre>" + newStringValue + "</pre>"
            String innerHtml = getContent().getInnerHtml();
            return innerHtml.substring("<pre>".length(),
                    innerHtml.length() - "</pre>".length());
        } else {
            throw new IllegalStateException("Unknown content mode");
        }
    }

    /**
     * Returns the current value of the data source converted using the current
     * locale.
     *
     * @return
     */
    private String getDataSourceValue() {
        return ConverterUtil.convertFromModel(
                getPropertyDataSource().getValue(), String.class,
                getConverter(), getLocale());
    }

    /**
     * Set the value of the label. Value of the label is the XML contents of the
     * label. Since Vaadin 7.2, changing the value of Label instance with that
     * method will fire ValueChangeEvent.
     *
     * @param newStringValue
     *            the New value of the label.
     */
    @Override
    public void setValue(String newStringValue) {
        if (newStringValue == null) {
            newStringValue = "";
        }

        if (getPropertyDataSource() == null) {
            if (!Objects.equals(getValue(), newStringValue)) {
                setDomValue(newStringValue, true);
            }
        } else {
            throw new IllegalStateException(
                    "Label is only a Property.Viewer and cannot update its data source");
        }
    }

    /**
     * Sets the text to the given value and optionally fires a value change
     * event.
     *
     * Does not check in any way if the value has changed, this is on the
     * callers responsibility.
     *
     * @param newValue
     *            the text to set
     * @param fireEvent
     *            true to fire a value change event, false otherwise
     */
    private void setDomValue(String newValue, boolean fireEvent) {
        if (getContentMode() == ContentMode.TEXT) {
            getContent().setInnerHtml("");
            getContent().getElement().setTextContent(newValue);
        } else if (getContentMode() == ContentMode.HTML) {
            getContent().setInnerHtml(newValue);
        } else if (getContentMode() == ContentMode.PREFORMATTED) {
            getContent().setInnerHtml("<pre>" + newValue + "</pre>");
        }

        if (fireEvent) {
            fireValueChange();
        }

    }

    /**
     * Gets the type of the Property.
     *
     * @see com.vaadin.data.Property#getType()
     */
    @Override
    public Class<String> getType() {
        return String.class;
    }

    /**
     * Gets the viewing data-source property.
     *
     * @return the data source property.
     * @see com.vaadin.data.Property.Viewer#getPropertyDataSource()
     */
    @Override
    public Property getPropertyDataSource() {
        return dataSource;
    }

    /**
     * Sets the property as data-source for viewing. Since Vaadin 7.2 a
     * ValueChangeEvent is fired if the new value is different from previous.
     *
     * @param newDataSource
     *            the new data source Property
     * @see com.vaadin.data.Property.Viewer#setPropertyDataSource(com.vaadin.data.Property)
     */
    @Override
    public void setPropertyDataSource(Property newDataSource) {
        // Stops listening the old data source changes
        if (dataSource != null && Property.ValueChangeNotifier.class
                .isAssignableFrom(dataSource.getClass())) {
            ((Property.ValueChangeNotifier) dataSource)
                    .removeValueChangeListener(this);
        }

        // Check if the current converter is compatible.
        if (newDataSource != null
                && !ConverterUtil.canConverterPossiblyHandle(getConverter(),
                        getType(), newDataSource.getType())) {
            // There is no converter set or there is no way the current
            // converter can be compatible.
            Converter<String, ?> c = ConverterUtil.getConverter(String.class,
                    newDataSource.getType(), getSession());
            setConverter(c);
        }

        dataSource = newDataSource;
        if (dataSource != null) {
            // Update the value from the data source. If data source was set to
            // null, retain the old value
            updateValueFromDataSource();
        }

        // Listens the new data source if possible
        if (dataSource != null && Property.ValueChangeNotifier.class
                .isAssignableFrom(dataSource.getClass())) {
            ((Property.ValueChangeNotifier) dataSource)
                    .addValueChangeListener(this);
        }
        markAsDirty();
    }

    /**
     * Gets the content mode of the Label.
     *
     * @return the Content mode of the label.
     *
     * @see ContentMode
     */
    public ContentMode getContentMode() {
        return contentMode;
    }

    /**
     * Sets the content mode of the Label.
     *
     * @param contentMode
     *            the New content mode of the label.
     *
     * @see ContentMode
     */
    public void setContentMode(ContentMode contentMode) {
        if (contentMode == null) {
            throw new IllegalArgumentException("Content mode can not be null");
        }
        if (this.contentMode == contentMode) {
            return;
        }

        String oldValue = getValue();
        this.contentMode = contentMode;
        setDomValue(oldValue, false);
    }

    /**
     * Adds the value change listener.
     *
     * @param listener
     *            the Listener to be added.
     * @see com.vaadin.data.Property.ValueChangeNotifier#addListener(com.vaadin.data.Property.ValueChangeListener)
     */
    @Override
    public void addValueChangeListener(Property.ValueChangeListener listener) {
        addListener(ValueChangeListener.class, listener);
    }

    /**
     * Removes the value change listener.
     *
     * @param listener
     *            the Listener to be removed.
     * @see com.vaadin.data.Property.ValueChangeNotifier#removeListener(com.vaadin.data.Property.ValueChangeListener)
     */
    @Override
    public void removeValueChangeListener(
            Property.ValueChangeListener listener) {
        removeListener(ValueChangeListener.class, listener);
    }

    /**
     * Emits the options change event.
     */
    protected void fireValueChange() {
        // Set the error message
        fireEvent(new Label.ValueChangeEvent(this));
    }

    /**
     * Listens the value change events from data source.
     *
     * @see com.vaadin.data.Property.ValueChangeListener#valueChange(Property.ValueChangeEvent)
     */
    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        updateValueFromDataSource();
    }

    private void updateValueFromDataSource() {
        // Update the internal value from the data source
        String newConvertedValue = getDataSourceValue();
        String currentDomValue = getDomValue();
        if (!Objects.equals(newConvertedValue, currentDomValue)) {
            setDomValue(newConvertedValue, true);
        }
    }

    @Override
    public void attach() {
        super.attach();
        localeMightHaveChanged();
    }

    @Override
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        localeMightHaveChanged();
    }

    private void localeMightHaveChanged() {
        if (getPropertyDataSource() != null) {
            updateValueFromDataSource();
        }
    }

    private String getComparableValue() {
        String stringValue = getValue();
        if (stringValue == null) {
            stringValue = "";
        }

        if (getContentMode() == ContentMode.HTML) {
            return stripTags(stringValue);
        } else {
            return stringValue;
        }

    }

    /**
     * Compares the Label to other objects.
     *
     * <p>
     * Labels can be compared to other labels for sorting label contents. This
     * is especially handy for sorting table columns.
     * </p>
     *
     * <p>
     * In RAW, PREFORMATTED and TEXT modes, the label contents are compared as
     * is. In XML, UIDL and HTML modes, only CDATA is compared and tags ignored.
     * If the other object is not a Label, its toString() return value is used
     * in comparison.
     * </p>
     *
     * @param other
     *            the Other object to compare to.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Label other) {

        String thisValue = getComparableValue();
        String otherValue = other.getComparableValue();

        return thisValue.compareTo(otherValue);
    }

    /**
     * Strips the tags from the XML.
     *
     * @param xml
     *            the String containing a XML snippet.
     * @return the original XML without tags.
     */
    private String stripTags(String xml) {

        final StringBuffer res = new StringBuffer();

        int processed = 0;
        final int xmlLen = xml.length();
        while (processed < xmlLen) {
            int next = xml.indexOf('<', processed);
            if (next < 0) {
                next = xmlLen;
            }
            res.append(xml.substring(processed, next));
            if (processed < xmlLen) {
                next = xml.indexOf('>', processed);
                if (next < 0) {
                    next = xmlLen;
                }
                processed = next + 1;
            }
        }

        return res.toString();
    }

    /**
     * Gets the converter used to convert the property data source value to the
     * label value.
     *
     * @return The converter or null if none is set.
     */
    public Converter<String, Object> getConverter() {
        return converter;
    }

    /**
     * Sets the converter used to convert the label value to the property data
     * source type. The converter must have a presentation type of String.
     *
     * @param converter
     *            The new converter to use.
     */
    public void setConverter(Converter<String, ?> converter) {
        this.converter = (Converter<String, Object>) converter;
        markAsDirty();
    }

    @Override
    protected HTML getContent() {
        return (HTML) super.getContent();
    }

    @Override
    protected Component initContent() {
        return new HTML("<div></div>");
    }

}
