import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverSideRoutes, listenerCollector } from "Frontend/generated/flow/Flow";

listenerCollector();

export const routes = [
  ...serverSideRoutes
] as RouteObject[];

export default createBrowserRouter(routes);
