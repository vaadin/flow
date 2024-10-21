package com.vaadin.flow.server.webpush;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WebPushMessageBuilder {
    // Required field
    private final String title;

    // Optional fields with default values
    private final List<WebPushAction> actions = new ArrayList<>();
    private String badge;
    private String body = "";
    private Serializable data = null;
    private WebPushDir dir = WebPushDir.AUTO;
    private String icon;
    private String image;
    private String lang = "";
    private boolean renotify = Boolean.FALSE;
    private boolean requireInteraction = Boolean.FALSE;
    private Boolean silent = null;
    private String tag = "";
    private long timestamp = System.currentTimeMillis();
    private final List<Integer> vibrate = new ArrayList<>();

    public WebPushMessageBuilder(String title) {
        this.title = title;
    }

    public WebPushMessageBuilder(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public WebPushMessageBuilder withAction(WebPushAction action) {
        actions.add(action);
        return this;
    }

    public WebPushMessageBuilder withActions(List<WebPushAction> actionList) {
        actions.clear();
        actions.addAll(actionList);
        return this;
    }

    public WebPushMessageBuilder withBadge(String badge) {
        this.badge = badge;
        return this;
    }

    public WebPushMessageBuilder withBody(String body) {
        this.body = body;
        return this;
    }

    public WebPushMessageBuilder withData(Serializable data) {
        this.data = data;
        return this;
    }

    public WebPushMessageBuilder withDir(WebPushDir dir) {
        this.dir = dir;
        return this;
    }

    public WebPushMessageBuilder withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public WebPushMessageBuilder withImage(String image) {
        this.image = image;
        return this;
    }

    public WebPushMessageBuilder withLang(String lang) {
        this.lang = lang;
        return this;
    }

    public WebPushMessageBuilder withRenotify(boolean renotify) {
        this.renotify = renotify;
        return this;
    }

    public WebPushMessageBuilder withRequireInteraction(boolean requireInteraction) {
        this.requireInteraction = requireInteraction;
        return this;
    }

    public WebPushMessageBuilder withSilent(Boolean silent) {
        this.silent = silent;
        return this;
    }

    public WebPushMessageBuilder withTag(String tag) {
        this.tag = tag;
        return this;
    }

    public WebPushMessageBuilder withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public WebPushMessageBuilder withVibrate(int vibrateValue) {
        vibrate.add(vibrateValue);
        return this;
    }

    public WebPushMessageBuilder withVibrates(List<Integer> vibrates) {
        this.vibrate.clear();
        this.vibrate.addAll(vibrates);
        return this;
    }

    public WebPushMessage build() {
        return new WebPushMessage(
                title,
                actions,
                badge,
                body,
                data,
                dir,
                icon,
                image,
                lang,
                renotify,
                requireInteraction,
                silent,
                tag,
                timestamp,
                vibrate
        );
    }
}
