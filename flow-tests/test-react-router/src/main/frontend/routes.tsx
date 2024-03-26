import ReactView from "Frontend/ReactView";
import { buildRoute } from "Frontend/generated/flow/Flow";
import { createBrowserRouter, RouteObject } from "react-router-dom";


export const routes = buildRoute([
    {path: '/react', element: <ReactView/>, handle: { title: 'React Test View' }}
] as RouteObject[]);

export default createBrowserRouter([...routes], { basename: new URL(document.baseURI).pathname });

