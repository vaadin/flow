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
