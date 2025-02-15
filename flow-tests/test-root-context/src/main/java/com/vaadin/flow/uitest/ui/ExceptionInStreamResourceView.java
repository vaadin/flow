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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("com.vaadin.flow.uitest.ui.ExceptionInStreamResourceView")
public class ExceptionInStreamResourceView extends Div {

    public ExceptionInStreamResourceView() {
        StreamResource faulty = new StreamResource(
                "you-should-not-see-this-download.pdf", () -> {
                    throw new IllegalStateException(
                            "Oops we cannot generate the stream");
                });
        Anchor anchor = new Anchor(faulty, "Click Here");
        anchor.getElement().setAttribute("download", true);
        anchor.setId("link");

        add(anchor);
    }
}
