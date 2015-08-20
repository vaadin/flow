package com.vaadin.server;

import java.io.Serializable;
import java.util.EventListener;

import javax.portlet.RenderResponse;

/**
 * Event listener notified when the bootstrap HTML is about to be generated and
 * send to the client. The bootstrap HTML is first constructed as an in-memory
 * DOM representation which registered listeners can modify before the final
 * HTML is generated.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public interface BootstrapFragmentListener extends EventListener, Serializable {
    /**
     * Lets this listener make changes to the fragment that makes up the actual
     * Vaadin application. In a typical Servlet deployment, this is the contents
     * of the HTML body tag. In a typical Portlet deployment, this is the HTML
     * that will be returned in a {@link RenderResponse}.
     *
     * @param response
     *            the bootstrap response that can modified to cause changes in
     *            the generated HTML.
     */
    public void modifyBootstrapFragment(BootstrapFragmentResponse response);
}
