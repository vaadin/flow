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
