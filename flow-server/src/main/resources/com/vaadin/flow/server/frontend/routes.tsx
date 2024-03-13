import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { buildRoute } from "Frontend/generated/flow/Flow";

export const routes = buildRoute();

// To define routes manually, use the following code as an example and remove the above code:
// let routing = [
//  {
//    element: <MainLayout />,
//    handle: { title: 'Main' },
//    children: [
//      { path: '/hilla', element: <HillaView />, handle: { title: 'Hilla' } }
//    ],
//  },
// ] as RouteObject[];
// export const routes = buildRoute(routing, routing[0].children);

export default createBrowserRouter(routes);
