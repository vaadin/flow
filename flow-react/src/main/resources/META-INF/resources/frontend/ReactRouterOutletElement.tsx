/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { Outlet } from 'react-router';
import { ReactAdapterElement } from "Frontend/generated/flow/ReactAdapter.js";
import React from "react";

class ReactRouterOutletElement extends ReactAdapterElement {
  public async connectedCallback() {
    await super.connectedCallback();
    this.style.display = 'contents';
  }

  protected render(): React.ReactElement | null {
    return <Outlet />;
  }

}

customElements.define('react-router-outlet', ReactRouterOutletElement);
