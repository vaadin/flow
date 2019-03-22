/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.TimingInfoReportedView", layout = ViewTestLayout.class)
public class TimingInfoReportedView extends Div {

  //@formatter:off
    private static final String REPORT_TIMINGS = "var element = this; setTimeout(function() {"
            + "function report(array){ "
            + "var div = document.createElement('div');"
            + "div.className='log';"
            + "element.appendChild(div); "
            + "if (array.length != 5) { "
            + "  div.appendChild(document.createTextNode('ERROR: expected 5 values, got '+array.length())); "
            + "}"
            + "for (i = 0; i < array.length; i++) { "
            + "  var value = 0+array[i];"
            + "  if ( value <0 || value >10000) {"
            + "     div.appendChild(document.createTextNode('ERROR: expected value "
            + " to be between 0 and 10000, was '+value)); "
            + "     return; "
            + "  } "
            + "}"
            + "div.appendChild(document.createTextNode('Timings ok'));"
            + "}; "
            + "report(window.Vaadin.Flow.clients[Object.keys(window.Vaadin.Flow.clients)].getProfilingData());"
            + "},0);";
  //@formatter:on

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getElement().executeJs(REPORT_TIMINGS);
        NativeButton button = new NativeButton("test request");
        button.addClickListener(
                event -> getElement().executeJs(REPORT_TIMINGS));
        add(button);
    }

}
