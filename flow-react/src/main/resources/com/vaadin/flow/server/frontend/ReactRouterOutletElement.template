import { Outlet } from 'react-router-dom';
import { ReactAdapterElement } from "./ReactAdapter";
import React from "react";

class ReactRouterOutletElement extends ReactAdapterElement {
  protected render(): React.ReactElement | null {
    return <Outlet />;
  }

}

customElements.define('react-router-outlet', ReactRouterOutletElement);
