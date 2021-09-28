package com.vaadin.experimental;

import java.io.Serializable;

public class Feature implements Serializable {

    private String title;
    private String id;
    private boolean serverRestartRequired;
    private String moreInfoTitle;
    private String moreInfoLink;
    private boolean enabled;

    public Feature(String title, String id, int githubIssue) {
        this(title, id, githubIssue, false);
    }

    public Feature(String title, String id, int githubIssue,
            boolean serverRestartRequired) {
        this.title = title;
        this.id = id;
        this.moreInfoTitle = "#" + githubIssue;
        this.moreInfoLink = "https://github.com/vaadin/flow/" + githubIssue;
        this.serverRestartRequired = serverRestartRequired;
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getMoreInfoLink() {
        return moreInfoLink;
    }

    public String getMoreInfoTitle() {
        return moreInfoTitle;
    }

    public boolean isServerRestartRequired() {
        return serverRestartRequired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
