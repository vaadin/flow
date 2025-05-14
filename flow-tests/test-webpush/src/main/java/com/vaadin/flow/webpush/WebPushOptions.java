package com.vaadin.flow.webpush;

import java.io.Serializable;
import java.util.List;

public record WebPushOptions(List<WebPushAction> actions,
                             String badge,
                             String body,
                             Serializable data,
                             String dir,
                             String icon,
                             String image,
                             String lang,
                             boolean renotify,
                             boolean requireInteraction,
                             boolean silent,
                             String tag,
                             long timestamp,
                             List<Integer> vibrate) implements Serializable {
}
