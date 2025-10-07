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

import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

@Route(value = "com.vaadin.flow.uitest.ui.PageView", layout = ViewTestLayout.class)
public class PageView extends AbstractDivView {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Input input = new Input();
        input.setId("input");
        input.clear();

        Div updateButton = new Div();
        updateButton.setId("button");
        updateButton.setText("Update page title");
        updateButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
        });

        Div overrideButton = new Div();
        overrideButton.setId("override");
        overrideButton.setText("Triggers two updates");
        overrideButton.addClickListener(e -> {
            getPage().setTitle(input.getValue());
            getPage().setTitle("OVERRIDDEN");
        });

        Div reloadButton = new Div();
        reloadButton.setId("reload");
        reloadButton.setText("Reloads the page");
        reloadButton.addClickListener(e -> {
            getPage().reload();
        });

        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest
                .getCurrent();
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        String url = httpServletRequest.getRequestURI().replace(
                PageView.class.getName(), BaseHrefView.class.getName());

        Div setLocationButton = new Div();
        setLocationButton.setId("setLocation");
        setLocationButton.setText("Set page location");
        setLocationButton.addClickListener(e -> getPage().setLocation(url));

        Div openButton = new Div();
        openButton.setId("open");
        openButton.setText("Open url in a new tab");
        openButton.addClickListener(e -> getPage().open(url));

        IFrame frame = new IFrame();
        frame.setId("newWindow");
        frame.setName("newWindow");
        Div openButton2 = new Div();
        openButton2.setId("openInIFrame");
        openButton2.setText("Open url in an IFrame");
        openButton2.addClickListener(e -> getPage().open(url, "newWindow"));

        add(input, updateButton, overrideButton, reloadButton,
                setLocationButton, openButton, openButton2, frame);
        add(new NativeButton("page.fetchURL", onClickEvent -> {
            getUI().ifPresent(ui -> ui.getPage().fetchCurrentURL(currentUrl -> {
                LoggerFactory.getLogger(PageView.class.getName())
                        .info(currentUrl.toString());
            }));
        }));

        NativeLabel directionLbl = new NativeLabel();
        directionLbl.setId("direction-value");
        add(directionLbl);

        Div fetchDirectionButton = new Div();
        fetchDirectionButton.setId("fetch-direction");
        fetchDirectionButton.setText("Fetch Page Direction");
        fetchDirectionButton.addClickListener(
                e -> getUI().ifPresent(ui -> ui.getPage().fetchPageDirection(
                        direction -> directionLbl.setText(direction.name()))));
        add(fetchDirectionButton);

        Div setRTLDirectionButton = new Div();
        setRTLDirectionButton.setId("set-RTL-direction");
        setRTLDirectionButton.setText("Set RTL Direction");
        setRTLDirectionButton.addClickListener(e -> getUI()
                .ifPresent(ui -> ui.setDirection(Direction.RIGHT_TO_LEFT)));
        add(setRTLDirectionButton);

        Div setLTRDirectionButton = new Div();
        setLTRDirectionButton.setId("set-LTR-direction");
        setLTRDirectionButton.setText("Set LTR Direction");
        setLTRDirectionButton.addClickListener(e -> getUI()
                .ifPresent(ui -> ui.setDirection(Direction.LEFT_TO_RIGHT)));
        add(setLTRDirectionButton);
    }

}
