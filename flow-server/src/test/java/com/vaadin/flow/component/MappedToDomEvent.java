/*
 * Copyright 2000-2025 Vaadin Ltd.
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
