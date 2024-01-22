import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverSideRoutes } from "Frontend/generated/flow/Flow";

export const routes = [
  ...serverSideRoutes
] as RouteObject[];

export default createBrowserRouter(routes);
