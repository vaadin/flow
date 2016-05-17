/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial.webcomponent;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Component;

@CodeFor("tutorial-webcomponent-basic.asciidoc")
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
