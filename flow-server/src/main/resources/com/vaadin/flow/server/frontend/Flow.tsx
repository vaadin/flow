/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import { Flow as _Flow } from "Frontend/generated/jar-resources/Flow.js";
import React, { useCallback, useEffect, useRef } from "react";
import {
    useLocation,
    useNavigate,
    RouteObject, useBlocker, createBrowserRouter
} from "react-router-dom";
//%viewsJsImport%
//%toReactRouterImport%

const flow = new _Flow({
    imports: () => import("Frontend/generated/flow/generated-flow-imports.js")
});

const router = {
    render() {
        return Promise.resolve();
    }
};

// ClickHandler for vaadin-router-go event is copied from vaadin/router click.js
// @ts-ignore
function getAnchorOrigin(anchor) {
    // IE11: on HTTP and HTTPS the default port is not included into
    // window.location.origin, so won't include it here either.
    const port = anchor.port;
    const protocol = anchor.protocol;
    const defaultHttp = protocol === 'http:' && port === '80';
    const defaultHttps = protocol === 'https:' && port === '443';
    const host = (defaultHttp || defaultHttps)
        ? anchor.hostname // does not include the port number (e.g. www.example.org)
        : anchor.host; // does include the port number (e.g. www.example.org:80)
    return `${protocol}//${host}`;
}

// @ts-ignore
export function fireRouterEvent(type, detail) {
    return !window.dispatchEvent(new CustomEvent(
        `vaadin-router-${type}`,
        {cancelable: type === 'go', detail}
    ));
}

// @ts-ignore

function extractLocation(event: MouseEvent): void | Location {
    // ignore the click if the default action is prevented
    if (event.defaultPrevented) {
        return;
    }

    // ignore the click if not with the primary mouse button
    if (event.button !== 0) {
        return;
    }

    // ignore the click if a modifier key is pressed
    if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) {
        return;
    }

    // find the <a> element that the click is at (or within)
    let anchor = event.target;
    const path = event.composedPath
        ? event.composedPath()
        // @ts-ignore
        : (event.path || []);

    // example to check: `for...of` loop here throws the "Not yet implemented" error
    for (let i = 0; i < path.length; i++) {
        const target = path[i];
        if (target.nodeName && target.nodeName.toLowerCase() === 'a') {
            anchor = target;
            break;
        }
    }

    // @ts-ignore
    while (anchor && anchor.nodeName.toLowerCase() !== 'a') {
        // @ts-ignore
        anchor = anchor.parentNode;
    }

    // ignore the click if not at an <a> element
    // @ts-ignore
    if (!anchor || anchor.nodeName.toLowerCase() !== 'a') {
        return;
    }

    // ignore the click if the <a> element has a non-default target
    // @ts-ignore
    if (anchor.target && anchor.target.toLowerCase() !== '_self') {
        return;
    }

    // ignore the click if the <a> element has the 'download' attribute
    // @ts-ignore
    if (anchor.hasAttribute('download')) {
        return;
    }

    // ignore the click if the <a> element has the 'router-ignore' attribute
    // @ts-ignore
    if (anchor.hasAttribute('router-ignore')) {
        return;
    }

    // ignore the click if the target URL is a fragment on the current page
    // @ts-ignore
    if (anchor.pathname === window.location.pathname && anchor.hash !== '') {
        // @ts-ignore
        window.location.hash = anchor.hash;
        return;
    }

    // ignore the click if the target is external to the app
    // In IE11 HTMLAnchorElement does not have the `origin` property
    // @ts-ignore
    const origin = anchor.origin || getAnchorOrigin(anchor);
    if (origin !== window.location.origin) {
        return;
    }

    // @ts-ignore
    const {pathname, search, hash} = anchor;
    return {pathname, search, hash};
}

type Location = Readonly<{
    pathname: string,
    search: string,
    hash: string,
}>;

function toPath({pathname, search, hash}: Location): string {
    return `${pathname}${search}${hash}`;
}

async function amendResult<T>(callback: () => void | Promise<HTMLElement | (() => void)>, fallbackAction: () => void): Promise<() => void> {
    const result = await callback();
    return result && !(result instanceof HTMLElement)
        ? result
        : fallbackAction;
}

function nop() {
}

type RouterContainer = Awaited<ReturnType<typeof flow.serverSideRoutes[0]["action"]>>;

export default function Flow() {
    const ref = useRef<HTMLOutputElement>(null);
    const navigate = useNavigate();
    const blocker = useBlocker(true);
    const {pathname, search, hash} = useLocation();

    const containerRef = useRef<RouterContainer | undefined>(undefined);

    const navigateEventHandler = useCallback((event: MouseEvent) => {
        const location = extractLocation(event);
        if (!location) {
            return;
        }

        if (event && event.preventDefault) {
            event.preventDefault();
        }

        navigate(toPath(location));
    }, [navigate]);

    const redirect = useCallback((path: string) => {
        return (() => {
            navigate(path, {replace: true});
        });
    }, [navigate]);


    useEffect(() => {
        return () => {
            containerRef.current?.parentNode?.removeChild(containerRef.current);
            containerRef.current = undefined;
        };
    }, []);

    useEffect(() => {
        if (blocker.state === 'blocked') {
            const prevent = () => blocker.reset;
            const {pathname, search} = blocker.location;
            Promise.resolve(containerRef.current?.onBeforeLeave?.call(containerRef?.current, {pathname, search}, {prevent}, router))
                .then((result: unknown) => {
                    if (typeof result === "function") {
                        result();
                    } else {
                        blocker.proceed();
                    }
                });
        } else {
            const prevent = () => nop;
            flow.serverSideRoutes[0].action({pathname, search})
                .then((container) => {
                    const outlet = ref.current?.parentNode;
                    if (outlet && outlet !== container.parentNode) {
                        outlet.append(container);
                        container.onclick = navigateEventHandler;
                        containerRef.current = container
                    }
                    return container.onBeforeEnter?.call(container, {pathname, search}, {prevent, redirect}, router);
                })
                .then((result: unknown) => {
                    if (typeof result === "function") {
                        result();
                    }
                });
        }
    }, [blocker.state, pathname, search]);
    return <output ref={ref} />;
}

export const serverSideRoutes = [
    { path: '/*', element: <Flow/> },
];

/**
 * Load the script for an exported WebComponent with the given tag
 *
 * @param tag name of the exported web-component to load
 *
 * @returns Promise(resolve, reject) that is fulfilled on script load
 */
export const loadComponentScript = (tag: String): Promise<void> => {
    return new Promise((resolve, reject) => {
        useEffect(() => {
            const script = document.createElement('script');
            script.src = `/web-component/${tag}.js`;
            script.onload = function() {
                resolve();
            };
            script.onerror = function(err) {
                reject(err);
            };
            document.head.appendChild(script);

            return () => {
                document.head.removeChild(script);
            }
        }, []);
    });
};

interface Properties {
    [key: string]: string;
}

type PopstateEventListener = (event: PopStateEvent) => boolean | void;

export function createRouter(routes: RouteObject[], opts: Parameters<typeof createBrowserRouter>[1] = {}) {
    const originalWindow = opts?.window ?? window;

    const popstateListeners = new WeakMap<PopstateEventListener, PopstateEventListener>();

    function ignoreVaadinPopstate(eventListener: PopstateEventListener): PopstateEventListener {
        return function (this: EventTarget, event: PopStateEvent) {
            if (event.state === 'vaadin-router-ignore') {
                return;
            }
            return eventListener.call(this, event);
        }
    }

    function addEventListener(type: string, eventListener: EventListener, options?: boolean | AddEventListenerOptions) {
        let listener = eventListener;
        if (type === 'popstate') {
            if (popstateListeners.has(eventListener)) {
                listener = popstateListeners.get(eventListener) as EventListener;
            } else {
                listener = ignoreVaadinPopstate(eventListener) as EventListener;
                popstateListeners.set(eventListener, listener);
            }
        }

        originalWindow.addEventListener(type, listener, options);
    }

    function removeEventListener(type: string, eventListener: EventListener, options?: boolean | EventListenerOptions) {
        let listener = eventListener;
        if (type === 'popstate' && popstateListeners.has(eventListener)) {
            listener = popstateListeners.get(eventListener) as EventListener;
            popstateListeners.delete(eventListener);
        }

        originalWindow.removeEventListener(type, listener, options);
    }

    const patchedWindow = new Proxy(originalWindow, {
        get(target: Window, name: string | symbol) {
            switch (name) {
                case 'addEventListener':
                    return addEventListener;
                case 'removeEventListener':
                    return removeEventListener;
                default:
                    const value: unknown = Reflect.get(target, name);
                    return typeof value === "function" ? value.bind(target) : value;
            }
        }
    });

    return createBrowserRouter(routes, {...opts, window: patchedWindow});
}

/**
 * Load WebComponent script and create a React element for the WebComponent.
 *
 * @param tag custom web-component tag name.
 * @param props optional Properties object to create element attributes with
 * @param onload optional callback to be called for script onload
 * @param onerror optional callback for error loading the script
 */
export const createWebComponent = (tag: string, props?: Properties, onload?: () => void, onerror?: (err:any) => void) => {
    loadComponentScript(tag).then(() => onload?.(), (err) => {
        if(onerror) {
            onerror(err);
        } else {
            console.error(`Failed to load script for ${tag}.`, err);
        }
    });

    if(props) {
        return React.createElement(tag, props);
    }
    return React.createElement(tag);
};

/**
 * Build routes for the application. Combines server side routes and FS routes.
 *
 * @param routes optional routes are for adding own route definition, giving routes will skip FS routes
 * @param serverSidePosition optional position where server routes should be put.
  *                          If non given they go to the root of the routes [].
 *
 * @returns RouteObject[] with combined routes
 */
export const buildRoute = (routes?: RouteObject[], serverSidePosition?: RouteObject[]): RouteObject[] => {
    let combinedRoutes = [] as RouteObject[];
    //%buildRouteFunction%
    if(serverSidePosition) {
        serverSidePosition.push(...serverSideRoutes);
    } else {
        combinedRoutes.push(...serverSideRoutes);
    }
    if(routes) {
        combinedRoutes.push(...routes);
    }
    return combinedRoutes;
};