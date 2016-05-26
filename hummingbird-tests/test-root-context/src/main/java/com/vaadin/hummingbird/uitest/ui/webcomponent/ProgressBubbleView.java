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
package com.vaadin.hummingbird.uitest.ui.webcomponent;

import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;
import com.vaadin.server.VaadinServlet;

/**
 * Example on how to use a web component.
 *
 * @author Vaadin Ltd
 */
public class ProgressBubbleView extends Div implements View {

    private static final String BACKGROUND = "background";
    ArrayList<ProgressBubble> bubbles = new ArrayList<>();

    /**
     * Creates a new view instance.
     */
    public ProgressBubbleView() {
        ProgressBubble bubble = new ProgressBubble(0, 100);
        bubble.getElement().getStyle().set(BACKGROUND, "green");
        bubbles.add(bubble);
        bubble = new ProgressBubble(0, 100);
        bubble.getElement().getStyle().set(BACKGROUND, "red");
        bubbles.add(bubble);
        bubble = new ProgressBubble(0, 100);
        bubble.getElement().getStyle().set(BACKGROUND, "blue");
        bubbles.add(bubble);
        bubble = new ProgressBubble(0, 100);
        bubble.getElement().getStyle().set(BACKGROUND, "purple");
        bubbles.add(bubble);

        Button makeProgress = new Button("Make progress");
        makeProgress.setId("makeProgress");
        makeProgress.addClickListener(e -> {
            bubbles.forEach(pb -> pb.setValue(pb.getValue() + 5));
        });

        Button increaseMax = new Button("Increase max value");
        increaseMax.setId("increaseMax");
        increaseMax.addClickListener(e -> {
            bubbles.forEach(pb -> pb.setMax(pb.getMax() * 2));
        });

        add(makeProgress, increaseMax);
        bubbles.forEach(this::add);
    }

    /**
     * Servlet for the application.
     */
    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(routerConfigurator = RouterConf.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }

    /**
     * Router configuration for the demo.
     */
    public static class RouterConf implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setRoute("", ProgressBubbleView.class);
        }
    }
}
