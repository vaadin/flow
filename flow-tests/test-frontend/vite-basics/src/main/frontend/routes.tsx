import { createBrowserRouter, RouteObject } from 'react-router';
import { serverSideRoutes } from 'Frontend/generated/flow/Flow';
import ReactComponents from './ReactComponents';
import ReactCustomComponents from "./ReactCustomComponents";

function build() {
  const routes = [
    { path: 'react-components',  element: <ReactComponents/> },
    { path: 'react-custom-components', element: <ReactCustomComponents /> },
    ...serverSideRoutes
  ] as RouteObject[];
  return {
    router: createBrowserRouter(routes),
    routes
  };
}
export const { router, routes } = build()
