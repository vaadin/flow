/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

@DomEvent("dom-event")
public class MappedToDomEvent extends ComponentEvent<Component> {

    private int someData;
    private String moreData;
    private boolean primitiveBoolean;
    private Boolean objectBoolean;

    public MappedToDomEvent(Component source) {
        super(source, false);
        someData = 32;
        moreData = "Default constructor";
    }

    public MappedToDomEvent(Component source, boolean fromClient) {
        super(source, fromClient);
        someData = 12;
        moreData = "Two arg constructor";
    }

    public MappedToDomEvent(Component source, boolean fromClient,
            @EventData("event.someData") int someData,
            @EventData("event.moreData") String moreData,
            @EventData("event.primitiveBoolean") boolean primitiveBoolean,
            @EventData("event.objectBoolean") Boolean objectBoolean) {
        super(source, fromClient);
        this.someData = someData;
        this.moreData = moreData;
        this.primitiveBoolean = primitiveBoolean;
        this.objectBoolean = objectBoolean;
    }

    public int getSomeData() {
        return someData;
    }

    public String getMoreData() {
        return moreData;
    }

    public boolean getPrimitiveBoolean() {
        return primitiveBoolean;
    }

    public Boolean getObjectBoolean() {
        return objectBoolean;
    }
}
