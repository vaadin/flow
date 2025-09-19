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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route("com.vaadin.flow.uitest.ui.StyleRemovalView")
public class StyleRemovalView extends Div {
    
    private Registration styleRegistration;
    private Registration styleRegistration1;
    private Registration styleRegistration2;
    
    public StyleRemovalView() {
        // Test div to apply styles to
        Div testDiv = new Div("Test Content");
        testDiv.setId("test-div");
        
        // Single style add/remove test
        NativeButton addStyle = new NativeButton("Add Style", e -> {
            if (styleRegistration == null) {
                // Create inline CSS that colors text red
                String css = "#test-div { color: red !important; }";
                styleRegistration = UI.getCurrent().getPage()
                        .addStyleSheet("data:text/css," + css);
            }
        });
        addStyle.setId("add-style");
        
        NativeButton removeStyle = new NativeButton("Remove Style", e -> {
            if (styleRegistration != null) {
                styleRegistration.remove();
                styleRegistration = null;
            }
        });
        removeStyle.setId("remove-style");
        
        // Multiple styles test
        NativeButton addStyle1 = new NativeButton("Add Style 1", e -> {
            if (styleRegistration1 == null) {
                String css = "#test-div { color: red !important; }";
                styleRegistration1 = UI.getCurrent().getPage()
                        .addStyleSheet("data:text/css," + css);
            }
        });
        addStyle1.setId("add-style-1");
        
        NativeButton removeStyle1 = new NativeButton("Remove Style 1", e -> {
            if (styleRegistration1 != null) {
                styleRegistration1.remove();
                styleRegistration1 = null;
            }
        });
        removeStyle1.setId("remove-style-1");
        
        NativeButton addStyle2 = new NativeButton("Add Style 2", e -> {
            if (styleRegistration2 == null) {
                String css = "#test-div { background-color: green !important; }";
                styleRegistration2 = UI.getCurrent().getPage()
                        .addStyleSheet("data:text/css," + css);
            }
        });
        addStyle2.setId("add-style-2");
        
        NativeButton removeStyle2 = new NativeButton("Remove Style 2", e -> {
            if (styleRegistration2 != null) {
                styleRegistration2.remove();
                styleRegistration2 = null;
            }
        });
        removeStyle2.setId("remove-style-2");
        
        add(testDiv);
        add(new Div(addStyle, removeStyle));
        add(new Div(addStyle1, removeStyle1, addStyle2, removeStyle2));
    }
}