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
