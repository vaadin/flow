/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import ReactView from "Frontend/ReactView";
import { serverSideRoutes } from "Frontend/generated/flow/Flow";
import { createBrowserRouter, RouteObject } from "react-router";

function build() {
    const routes = [
        {path: '/react', element: <ReactView/>, handle: { title: 'React Test View' }},
        ...serverSideRoutes
    ] as RouteObject[];
    return {
        router: createBrowserRouter([...routes], { basename: new URL(document.baseURI).pathname }),
        routes
    };
}
export const { router, routes } = build()

