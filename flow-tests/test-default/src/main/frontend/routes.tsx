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

