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

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.UIsCollectedWithBeaconAPIView")
public class UIsCollectedWithBeaconAPIView extends Div {

    static int viewcount = 0;

    Div count = new Div();

    public UIsCollectedWithBeaconAPIView() {
        viewcount++;
        add(count);
        count.setId("uis");
        NativeButton showUisNumber = new NativeButton("Update",
                event -> updateCount());
        add(showUisNumber);
        updateCount();
    }

    private void updateCount() {
        count.setText("" + viewcount);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        viewcount--;
    }

}
