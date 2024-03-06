import { createBrowserRouter, RouteObject } from 'react-router-dom';
import { serverSideRoutes } from "Frontend/generated/flow/Flow";
import { getDefaultReactRoutes } from "Frontend/generated/flow/Flow";


export const routes = getDefaultReactRoutes();
export default createBrowserRouter(routes, {basename: new URL(document.baseURI).pathname });
