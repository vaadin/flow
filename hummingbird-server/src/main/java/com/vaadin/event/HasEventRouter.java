package com.vaadin.event;

import java.io.Serializable;

public interface HasEventRouter extends Serializable {
    public EventRouter getEventRouter();
}
