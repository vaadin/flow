import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverSideRoutes } from "Frontend/generated/flow/Flow";
import { toReactRouter } from '@vaadin/hilla-file-router/runtime.js';
import views from 'Frontend/generated/views.js';

// @ts-ignore
const route = toReactRouter(views);
export const routes = [...serverSideRoutes] as RouteObject[]
if(route.children && route.children.length > 0) {
  route.children?.push(...serverSideRoutes);
  routes.pop();
  routes.push(route);
}

// To define routes manually, use the following code as a example and remove the above code:
// export const routes = [
//  {
//    element: <MainLayout />,
//    handle: { title: 'Main' },
//    children: [
//      { path: '/hilla', element: <HillaView />, handle: { title: 'Hilla' } },
//      ...serverSideRoutes
//    ],
//  },
// ] as RouteObject[];

export default createBrowserRouter(routes);
