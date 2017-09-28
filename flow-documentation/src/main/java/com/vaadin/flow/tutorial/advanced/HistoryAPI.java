/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.advanced;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.tutorial.annotations.CodeFor;

import elemental.json.Json;
import elemental.json.JsonValue;

@CodeFor("advanced/tutorial-history-api.asciidoc")
public class HistoryAPI {

    void tutorialCode() {
        History history = UI.getCurrent().getPage().getHistory();

        history.back(); // navigates back to the previous entry

        history.forward(); // navigates forward to the next entry

        history.go(-2); // navigates back two entries
        history.go(1); // equal to history.forward();
        history.go(0); // will reload the current page

        history.setHistoryStateChangeHandler(this::onHistoryStateChange);

        // adds a new history entry for location "home", no state
        history.pushState(null, "home");

        // replaces the current entry with location "about" and a state object
        JsonValue state = Json.create("preview-mode");
        history.replaceState(state, "about");
    }

    private void onHistoryStateChange(HistoryStateChangeEvent event) {
        // site base url is www.abc.com/
        // user navigates back from abc.com/dashboard to abc.com/home
        event.getLocation().getPath(); // returns "home"
    }

}
