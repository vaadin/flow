package com.vaadin.tests.util;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.HTML;
import com.vaadin.ui.VerticalLayout;

public class Log extends VerticalLayout {
    List<HTML> eventLabels = new ArrayList<HTML>();
    private boolean numberLogRows = true;
    private int nextLogNr = 1;

    public Log(int nr) {
        for (int i = 0; i < nr; i++) {
            HTML l = createEventLabel("&nbsp;");
            l.setId("Log_row_" + i);
            eventLabels.add(l);
            addComponent(l);
        }
        setId("Log");
        setCaption("Events:");
    }

    /**
     * Clears the rows and reset the row number to zero.
     */
    public Log clear() {
        for (HTML l : eventLabels) {
            l.setInnerHtml("&nbsp;");
        }

        nextLogNr = 0;
        return this;
    }

    public Log log(String event) {
        int nr = eventLabels.size();
        for (int i = 0; i < nr; i++) {
            eventLabels.get(i).setId("Log_row_" + i);
        }
        removeComponent(getComponent(0));
        String msg = event;
        if (numberLogRows) {
            msg = nextLogNr + ". " + msg;
            nextLogNr++;
        }
        addComponent(createEventLabel(msg));
        System.out.println(event);
        return this;
    }

    private HTML createEventLabel(String html) {
        HTML l = new HTML(html);
        return l;
    }

    public Log setNumberLogRows(boolean numberLogRows) {
        this.numberLogRows = numberLogRows;
        return this;
    }

}
