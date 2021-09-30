/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.experimental;

import java.io.Serializable;

/**
 * Information about a feature available behind a flag.
 */
public class Feature implements Serializable {

    private String title;
    private String id;
    private String moreInfoTitle;
    private String moreInfoLink;
    private boolean enabled;

    /**
     * Creates a new feature with the given options.
     * 
     * @param title       the title of the feature
     * @param id          the unique id of the feature
     * @param githubIssue the issue describing the feature on a high level
     */
    public Feature(String title, String id, int githubIssue) {
        this.title = title;
        this.id = id;
        this.moreInfoTitle = "#" + githubIssue;
        this.moreInfoLink = "https://github.com/vaadin/flow/" + githubIssue;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
