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
package com.vaadin.flow.testutil;

import java.util.List;

public abstract class AbstractBrowserConsoleTest extends ChromeBrowserTest {

    @Override
    protected void open(String... parameters) {
        super.open(parameters);

        getCommandExecutor().executeScript("window.logs = [];"
                + "var origConsole = window.console; window.console = {"
                + " debug:function(msg){ origConsole.debug(msg); window.logs.push(msg);},"
                + " log:function(msg){ origConsole.log(msg); window.logs.push(msg);},"
                + " error:function(msg){ origConsole.error(msg); window.logs.push(msg);},"
                + " info:function(msg){ origConsole.info(msg); window.logs.push(msg);},"
                + " warn:function(msg){ origConsole.warn(msg); window.logs.push(msg);}"
                + "};");
    }

    protected List<?> getBrowserLogs(boolean reset) {
        if (reset) {
            return (List<?>) getCommandExecutor().executeScript(
                    "var result = window.logs; window.logs=[]; return result || [];");
        } else {
            return (List<?>) getCommandExecutor()
                    .executeScript("return window.logs || [];");
        }
    }

}
