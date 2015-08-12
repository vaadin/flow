package com.vaadin.tests.components;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.shared.ui.ui.UIState.PushConfigurationState;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.PushConfiguration;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public abstract class AbstractTestUI extends UI {

    @Override
    public void init(VaadinRequest request) {
        getPage().setTitle(getClass().getName());

        Label label = new Label(getTestDescription(), ContentMode.HTML);
        label.setWidth("100%");

        VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setMargin(true);
        setContent(rootLayout);

        layout = new VerticalLayout();

        rootLayout.addComponent(label);
        rootLayout.addComponent(layout);
        ((VerticalLayout) getContent()).setExpandRatio(layout, 1);

        setTransport(request);

        setup(request);
    }

    /**
     * Sets the push transport according to the transport= URL parameter if such
     * is given. Supports transport=xhr (disables push), transport=websocket
     * (forces websocket into use), transport=long-polling(forces long-polling
     * into use). Using ?transport=xyz disables the fallback transport.
     * 
     * @param request
     *            The UI init request
     */
    protected void setTransport(VaadinRequest request) {
        String transport = request.getParameter("transport");
        PushConfiguration config = getPushConfiguration();

        if ("xhr".equals(transport)) {
            config.setPushMode(PushMode.DISABLED);
        } else if ("websocket".equals(transport)) {
            enablePush(Transport.WEBSOCKET);
        } else if ("long-polling".equals(transport)) {
            enablePush(Transport.LONG_POLLING);
        } else if (transport != null) {
            throw new IllegalArgumentException("Unknown transport value '"
                    + transport
                    + "'. Supported are xhr,websocket,streaming,long-polling");
        }
    }

    protected void enablePush(Transport transport) {
        PushConfiguration config = getPushConfiguration();
        if (!config.getPushMode().isEnabled()) {
            config.setPushMode(PushMode.AUTOMATIC);
        }
        config.setTransport(transport);
        // Ensure no fallback is used
        getPushConfiguration().setParameter(
                PushConfigurationState.FALLBACK_TRANSPORT_PARAM, "none");
    }

    /**
     * This method is inherited from the super class, but it should generally
     * not be used. If you want to just add components to your test, use e.g.
     * {@link #addComponent(Component)} instead to add the component to the
     * layout used by this UI. If you don't want to use the top-level layout
     * used by this class, you instead inherit directly from UI.
     * 
     * @deprecated Use {@link #addComponent(Component)} or inherit from UI
     *             instead.
     */
    @Override
    @Deprecated
    public void setContent(Component content) {
        // Overridden just to deprecate
        super.setContent(content);
    }

    private VerticalLayout layout;

    protected VerticalLayout getLayout() {
        return layout;
    }

    protected abstract void setup(VaadinRequest request);

    public void addComponent(Component c) {
        getLayout().addComponent(c);
    }

    public void addComponents(Component... c) {
        getLayout().addComponents(c);
    }

    public void removeComponent(Component c) {
        getLayout().removeComponent(c);
    }

    public void replaceComponent(Component oldComponent,
            Component newComponent) {
        getLayout().replaceComponent(oldComponent, newComponent);
    }

    protected void addButton(String caption, Button.ClickListener listener) {
        Button button = new Button(caption);
        button.addClickListener(listener);
        addComponent(button);
    }

    protected String getTestDescription() {
        return null;
    };

    protected Integer getTicketNumber() {
        return null;
    };

    protected WebBrowser getBrowser() {
        return getSession().getBrowser();
    }

    /**
     * Execute the provided runnable on the UI thread as soon as the current
     * request has been sent.
     */
    protected void runAfterResponse(final Runnable runnable) {
        // Immediately start a thread that will start waiting for the session to
        // get unlocked.
        new Thread() {
            @Override
            public void run() {
                accessSynchronously(runnable);
            }
        }.start();
    }
}
