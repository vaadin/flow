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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("advanced/tutorial-stream-resources.asciidoc")
public class StreamResources {

    void tutorialCode() {
        StreamReceiver streamReceiver = new StreamReceiver(
                getElement().getNode(), "upload", getStreamVariable());
        getElement().setAttribute("target", streamReceiver);
    }

    private Element getElement() {
        return null;
    }

    public StreamVariable getStreamVariable() {
        return null;
    }
}
