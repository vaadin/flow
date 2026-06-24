/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {useNavigate} from "react-router";
import {
    ReactAdapterElement,
    RenderHooks
} from "Frontend/generated/flow/ReactAdapter";

 class NavigateView extends ReactAdapterElement {
     protected render(hooks: RenderHooks): React.ReactElement | null {
         const navigate = useNavigate();

         return (
             <>
                 <p id="react">This is a simple view for a React route</p>
                 <button id="react-navigate" onClick={() => navigate("com.vaadin.flow.RouterView"!)}>
                     Navigate button
                 </button>
             </>
         );
     }
 }

customElements.define('navigate-view', NavigateView);