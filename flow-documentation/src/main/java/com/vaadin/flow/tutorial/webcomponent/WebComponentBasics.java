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
package com.vaadin.flow.tutorial.webcomponent;

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Tag;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.Component;

@CodeFor("web-components/tutorial-webcomponent-basic.asciidoc")
public class WebComponentBasics {
    @Tag("paper-slider")
    @HtmlImport("bower_components/paper-slider/paper-slider.html")
    public class PaperSlider extends Component {
        public PaperSlider() {
        }
    }

    public class PaperSliderView extends Div implements View {
        public PaperSliderView() {
            add(new PaperSlider());
        }
    }

}
