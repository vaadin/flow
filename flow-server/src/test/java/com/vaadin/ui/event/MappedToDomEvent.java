package com.vaadin.ui.event;

import com.vaadin.ui.Component;

@DomEvent("dom-event")
public class MappedToDomEvent extends ComponentEvent<Component> {

    private int someData;
    private String moreData;

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
            @EventData("event.moreData") String moreData) {
        super(source, fromClient);
        this.someData = someData;
        this.moreData = moreData;
    }

    public int getSomeData() {
        return someData;
    }

    public String getMoreData() {
        return moreData;
    }
}
