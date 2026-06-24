/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { createBrowserRouter, RouteObject } from 'react-router';
import { serverSideRoutes } from 'Frontend/generated/flow/Flow';
import ReactComponents from './ReactComponents';

function build() {
  const routes = [
    { path: 'react-components',  element: <ReactComponents/> },
    ...serverSideRoutes
  ] as RouteObject[];
  return {
    router: createBrowserRouter(routes),
    routes
  };
}
export const { router, routes } = build()
