/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

import java.awt.Button;

/**
 * View for testing IFrame.reload(), based on
 * https://github.com/vaadin/flow/issues/6808
 *
 * @since 14.0
 */
@Route(value = "com.vaadin.flow.uitest.ui.IFrameTestView", layout = ViewTestLayout.class)
public class IFrameTestView extends AbstractDivView {

    public IFrame frame = new IFrame();
    private Button button = new Button();

    public IFrameTestView() {
        /*
         * The test consists of creating a view with an IFrame and a button.
         * The IFrame contains initially a.html. Upon pressing the button,
         * it loads b. html into the IFrame. The test then verifies that
         * b.html is loaded in the IFrame.
         */
        frame.setSrc("a.html");
        button.setEnabled(true);

    }

    public void handleButtonClick() {
        frame.setSrc("b.html");
        frame.reload();
    }

}
