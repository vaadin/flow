/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JsFunctionView", layout = ViewTestLayout.class)
public class JsFunctionView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div captureResult = new Div();
        captureResult.setId("captureResult");
        Div argsResult = new Div();
        argsResult.setId("argsResult");
        Div elementCaptureResult = new Div();
        elementCaptureResult.setId("elementCaptureResult");
        Div thisRespectResult = new Div();
        thisRespectResult.setId("thisRespectResult");

        NativeButton captureButton = createButton("Run capture function",
                "captureButton", e -> {
                    JsFunction concat = JsFunction.of("return $0 + ' ' + $1;",
                            "Hello", "World");
                    captureResult.getElement()
                            .executeJs("this.textContent = $0();", concat);
                });

        NativeButton argsButton = createButton("Run function with arguments",
                "argsButton", e -> {
                    JsFunction setText = JsFunction
                            .of("return prefix + ':' + suffix;")
                            .withArguments("prefix", "suffix");
                    argsResult.getElement().executeJs(
                            "this.textContent = $0('alpha', 'beta');", setText);
                });

        NativeButton elementCaptureButton = createButton(
                "Run function capturing element", "elementCaptureButton", e -> {
                    JsFunction mutate = JsFunction.of(
                            "$0.textContent = 'mutated via capture';",
                            elementCaptureResult.getElement());
                    getElement().executeJs("$0();", mutate);
                });

        NativeButton thisRespectButton = createButton(
                "Run function with caller-supplied this", "thisRespectButton",
                e -> {
                    JsFunction setOwnText = JsFunction
                            .of("this.textContent = msg;").withArguments("msg");
                    thisRespectResult.getElement().executeJs(
                            "$0.call(this, 'this is the host element');",
                            setOwnText);
                });

        add(captureButton, argsButton, elementCaptureButton, thisRespectButton,
                captureResult, argsResult, elementCaptureResult,
                thisRespectResult);
    }
}
