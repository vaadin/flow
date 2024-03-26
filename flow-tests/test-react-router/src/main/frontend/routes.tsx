import ReactView from "Frontend/ReactView";
import { serverSideRoutes } from "Frontend/generated/flow/Flow";
import { createBrowserRouter, RouteObject } from "react-router-dom";


export const routes = [
    {path: '/react', element: <ReactView/>, handle: { title: 'React Test View' }},
    ...serverSideRoutes
] as RouteObject[];

export default createBrowserRouter([...routes], { basename: new URL(document.baseURI).pathname });

