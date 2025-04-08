package com.vaadin.flow.server.streams;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class TransferRequest implements Serializable {
    private final VaadinRequest request;
    private final VaadinResponse response;
    private final VaadinSession session;

    private final String fileName;
    private String contentType;

    private Component owningComponent;

    private long size = -1;
    private long transferInterval;

    /**
     * Create a new transfer request with required data.
     *
     * @param request
     *            current request
     * @param response
     *            current response to write response data to
     * @param session
     *            current session
     * @param fileName
     *            defined transfered file name
     */
    public TransferRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, String fileName) {
        this.request = request;
        this.response = response;
        this.session = session;
        this.fileName = fileName;
    }

    /**
     * Set the owning component for the transfer event from the element instance
     * if a component is available.
     *
     * @param owningElement
     *            owning element for the event
     * @return this Event instance
     */
    TransferRequest withOwningComponent(Element owningElement) {
        if (owningElement != null) {
            Optional<Component> component = owningElement.getComponent();
            component.ifPresent(this::withOwningComponent);
        }
        return this;
    }

    /**
     * Set the owning component for the transfer event.
     *
     * @param owningComponent
     *            owning component for the event
     * @return this Event instance
     */
    TransferRequest withOwningComponent(Component owningComponent) {
        this.owningComponent = owningComponent;
        return this;
    }

    /**
     * Set the TransferRequest content type.
     *
     * @param contentType
     *            content type of the event content
     * @return this Event instance
     */
    TransferRequest withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public void withTransferInterval(long transferInterval) {
        this.transferInterval = transferInterval;
    }

    public long getTransferInterval() {
        return transferInterval;
    }

    /**
     * Get {@link VaadinRequest} for transfer event.
     *
     * @return vaadin request
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Get {@link VaadinResponse} for transfer event.
     *
     * @return vaadin response
     */
    public VaadinResponse getResponse() {
        return response;
    }

    /**
     * Get {@link VaadinSession} for transfer event.
     *
     * @return vaadin session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Get the set file name.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName == null ? "" : fileName;
    }

    /**
     * Get the content type for the data to transfer.
     *
     * @return set content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get owner {@link Component} for this event.
     *
     * @return owning component or null in none defined
     */
    public Component getOwningComponent() {
        return owningComponent;
    }

    /**
     * Get the UI instance for this request.
     *
     * @return Current UI
     */
    public UI getUI() {
        return getOwningComponent().getUI().orElseGet(UI::getCurrent);
    }

    void withSize(long size) {
        this.size = size;
    }

    /**
     * Get the size of the data to transfer, if the size is known beforehand
     * (e.g. for file transfers), or -1 if the size is unknown (e.g. when
     * reading from input streams).
     *
     * @return size of the data to transfer, or -1 if size is unknown.
     */
    public long getSize() {
        return size;
    }
}
