package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.ui.Transport;

@Push(transport = Transport.WEBSOCKET)
public class PushWebsocketDeadlockUI extends AbstractTestUIWithLog {

    // Test for https://dev.vaadin.com/ticket/18436
    // Needs breakpoints to test, see ticket for more information
    // Can reproduce on Tomcat 8, can't seem to reproduce using
    // DevelopmentServerLauncher

    // Rough steps to reproduce
    // 1. Open test in a new Chrome window
    // 2. Set breakpoint in PushHandler.connectionLost
    // 3. Set breakpoint in UI.close
    // 4. Set breakpoint in PushRequestHandler.handleRequest
    // 5. Click the "schedule UI close" button
    // 6. Close the Chrome window before the 5s timeout expires and ensure it
    // really closes
    // 7. Wait for three threads to hit their breakpoints
    // 8. Continue/step forward in proper order (see ticket)

    @Override
    protected void init(VaadinRequest request) {
        WrappedSession wrappedSession = getSession().getSession();
        request.getService().addSessionDestroyListener(event -> System.out
                .println("Session " + event.getSession() + " destroyed"));
        Div div = new Div();
        div.setText("Session timeout is "
                + wrappedSession.getMaxInactiveInterval() + "s");
        add(div);

        NativeButton button = new NativeButton("Invalidate session");
        button.addClickListener(event -> {
            System.out.println("invalidating " + getSession()
                    + " for http session " + getSession().getSession().getId());
            getSession().getSession().invalidate();
            System.out.println("invalidated " + getSession());
        });
        add(button);
        button = new NativeButton("Close UI");
        button.addClickListener(event -> {
            System.out.println("closing UI " + getUIId() + " in session "
                    + getSession() + " for http session "
                    + getSession().getSession().getId());
            close();
        });
        add(button);
        button = new NativeButton("Schedule Close UI (5s delay)");
        button.addClickListener(event -> {
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Breakpoint here
                access(() -> {
                    close();
                    System.out
                            .println("closing UI " + getUIId() + " in session "
                                    + getSession() + " for http session "
                                    + getSession().getSession().getId());
                });
            }).start();
        });
        add(button);
        button = new NativeButton("Slow (5s) operation");
        button.addClickListener(event -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Div label = new Div();
            label.setText("Slow operation done");
            add(label);
        });

        add(button);
    }

}
