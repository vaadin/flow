package com.vaadin.tests.applicationcontext;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.tests.util.Log;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class ChangeSessionId extends AbstractTestUIWithLog {

    private Log log = new Log(5);
    Button loginButton = new Button("Change session");
    boolean requestSessionSwitch = false;

    @Override
    public void setup(VaadinRequest res) {
        add(loginButton);
        add(new Button("Show session id", new Button.ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                logSessionId();
            }
        }));

        loginButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                String oldSessionId = getSessionId();
                VaadinService
                        .reinitializeSession(VaadinService.getCurrentRequest());
                String newSessionId = getSessionId();
                if (oldSessionId.equals(newSessionId)) {
                    log.log("FAILED! Both old and new session id is "
                            + newSessionId);
                } else {
                    log.log("Session id changed successfully from "
                            + oldSessionId + " to " + newSessionId);
                }

            }
        });
        logSessionId();
    }

    private void logSessionId() {
        log.log("Session id: " + getSessionId());
    }

    protected String getSessionId() {
        return getSession().getSession().getId();
    }

    @Override
    protected String getTestDescription() {
        return "Tests that the session id can be changed to prevent session fixation attacks";
    }

    @Override
    protected Integer getTicketNumber() {
        return 6094;
    }

}
