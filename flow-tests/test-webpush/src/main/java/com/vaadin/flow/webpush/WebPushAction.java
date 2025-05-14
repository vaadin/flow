package com.vaadin.flow.webpush;

import java.io.Serializable;

public record WebPushAction(String action, String title, String icon) implements Serializable {
}
