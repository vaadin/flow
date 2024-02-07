import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverSideRoutes } from 'Frontend/generated/flow/Flow';
import ReactComponents from './ReactComponents';

export const routes = [
  { path: 'react-components',  element: <ReactComponents/> },
  ...serverSideRoutes
] as RouteObject[];

export default createBrowserRouter(routes);
