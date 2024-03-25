import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverRoute } from 'Frontend/generated/flow/server-route';
import ReactComponents from './ReactComponents';

export const routes = [
  { path: 'react-components',  element: <ReactComponents/> },
  ...serverRoute
] as RouteObject[];

export default createBrowserRouter(routes);
