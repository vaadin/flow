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

package com.vaadin.flow;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Test;

public class InvalidLocationIT extends ChromeBrowserTest {

    // #9443
    @Test
    public void invalidCharactersOnPath_UiNotServed() {
        open();

        checkLogsForErrors(msg -> msg
                .contains("the server responded with a status of 400"));
    }

    @Override
    protected String getTestPath() {
        return "/view/..**";
    }
}
