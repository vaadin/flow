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
import React, { useEffect, useRef } from "react";
import {
    matchRoutes,
    NavigateFunction,
    useLocation,
    useNavigate
} from "react-router-dom";
import { routes } from "%routesJsImportPath%";

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
function vaadinRouterGlobalClickHandler(event) {
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
        : (event.path || []);

    // example to check: `for...of` loop here throws the "Not yet implemented" error
    for (let i = 0; i < path.length; i++) {
        const target = path[i];
        if (target.nodeName && target.nodeName.toLowerCase() === 'a') {
            anchor = target;
            break;
        }
    }

    while (anchor && anchor.nodeName.toLowerCase() !== 'a') {
        anchor = anchor.parentNode;
    }

    // ignore the click if not at an <a> element
    if (!anchor || anchor.nodeName.toLowerCase() !== 'a') {
        return;
    }

    // ignore the click if the <a> element has a non-default target
    if (anchor.target && anchor.target.toLowerCase() !== '_self') {
        return;
    }

    // ignore the click if the <a> element has the 'download' attribute
    if (anchor.hasAttribute('download')) {
        return;
    }

    // ignore the click if the <a> element has the 'router-ignore' attribute
    if (anchor.hasAttribute('router-ignore')) {
        return;
    }

    // ignore the click if the target URL is a fragment on the current page
    if (anchor.pathname === window.location.pathname && anchor.hash !== '') {
        lastNavigation = anchor.pathname;
        lastNavigationSearch = anchor.search;
        window.location.hash = anchor.hash;
        return;
    }

    // ignore the click if the target is external to the app
    // In IE11 HTMLAnchorElement does not have the `origin` property
    const origin = anchor.origin || getAnchorOrigin(anchor);
    if (origin !== window.location.origin) {
        return;
    }

    // if none of the above, convert the click into a navigation event
    const {href, baseURI, search, hash} = anchor;
    // Normalize away base from pathname. e.g. /react should remove base /view from /view/react
    let normalizedPathname = href.replace(search,'').replace(baseURI, '').replace(hash, '');
    normalizedPathname = normalizedPathname.startsWith("/") ? normalizedPathname : "/" + normalizedPathname;
    if (fireRouterEvent('go', {pathname: normalizedPathname, search, hash, clientNavigation: true})) {
        event.preventDefault();
        // for a click event, the scroll is reset to the top position.
        if (event && event.type === 'click') {
            window.scrollTo(0, 0);
        }
    }
}

// We can't initiate useNavigate() from outside React component, so we store it here for use in the navigateEvent.
let navigation: NavigateFunction | ((arg0: any, arg1: { replace: boolean; }) => void);
let mountedContainer: Awaited<ReturnType<typeof flow.serverSideRoutes[0]["action"]>> | undefined = undefined;
let lastNavigation: string;
let lastNavigationSearch: string;
let prevNavigation: string;
let popstateListener: { type: string, listener: EventListener, useCapture: boolean };

// @ts-ignore
function navigateEventHandler(event) {
    if (event && event.preventDefault) {
        event.preventDefault();
    }
    // Normalize path against baseURI if href available.
    let normalizedPathname = event.detail.href ?
        event.detail.href.replace(event.detail.search ,'').replace(document.baseURI, '').replace(event.detail.hash, '') :
        event.detail.pathname;
    normalizedPathname = normalizedPathname.startsWith("/") ? normalizedPathname : "/" + normalizedPathname;

    // @ts-ignore
    let matched = matchRoutes(Array.from(routes), normalizedPathname);
    prevNavigation = lastNavigation;

    // if navigation event route targets a flow view do beforeEnter for the
    // target path. Server will then handle updates and postpone as needed.
    // @ts-ignore
    if(matched && matched.filter(path => path.route?.element?.type?.name === Flow.name).length >= 1) {
        if (mountedContainer?.onBeforeEnter) {
            // onBeforeEvent call will handle the Flow navigation
            mountedContainer.onBeforeEnter(
                {
                    pathname: event.detail.pathname,
                    search: event.detail.search
                },
                {
                    prevent() {
                        window.history.pushState(window.history.state, '', prevNavigation);
                        window.dispatchEvent(new PopStateEvent('popstate', {state: 'vaadin-router-ignore'}));
                    },
                    // @ts-ignore
                    redirect: (path) => {
                        navigation(path, {replace: false});
                    },
                    continue: () => {
                        let path = event.detail.pathname;
                        if(event.detail.search) path += event.detail.search;
                        if(event.detail.hash) path += event.detail.hash;
                        if(window.location.pathname !== path) {
                            window.history.pushState(window.history.state, '', path);
                            window.dispatchEvent(new PopStateEvent('popstate', {state: 'vaadin-router-ignore'}));
                        }
                    }
                },
                router,
            );
        }
    } else {
        // Navigating to a non flow view. If beforeLeave set call that before
        // navigation. If not postponed clear + navigate will be executed.
        if (mountedContainer?.onBeforeLeave) {
            mountedContainer?.onBeforeLeave({pathname: event.detail.pathname, search: event.detail.search}, {
                prevent() {},
                continue() {
                    mountedContainer?.parentNode?.removeChild(mountedContainer);
                    mountedContainer = undefined;
                    // clientNavigation flag denotes navigation to a client route through
                    // a link or server navigate. If clientNavigation is not given the
                    // navigation is through a history event and we only call the router popstate event.
                    if (event.detail.clientNavigation) {
                        navigation(normalizedPathname, {replace: false});
                    } else {
                        popstateListener.listener(new PopStateEvent('popstate', {state: 'vaadin-router-ignore'}));
                    }
                }
            }, router);
        } else {
            // Navigate to a non flow view. Clean nodes and undefine container.
            mountedContainer?.parentNode?.removeChild(mountedContainer);
            mountedContainer = undefined;
            navigation(event.detail.pathname, {replace: false});
        }
    }
    lastNavigation = event.detail.pathname;
    lastNavigationSearch = event.detail.search;
}

function popstateHandler(event: PopStateEvent) {
    fireRouterEvent('location-changed', {
        location: window.location
    });

    if (event.state === 'vaadin-router-ignore') {
        // Update last nav path to keep it on track. Otherwise it would be
        // updated in vaadin-router-go event handler if needed.
        lastNavigation = window.location.pathname;
        lastNavigationSearch = window.location.search;
        return;
    }
    const {pathname, search, hash} = window.location;
    if(pathname === lastNavigation && search === lastNavigationSearch) {
        return;
    }
    // @ts-ignore
    window.dispatchEvent(new CustomEvent('vaadin-router-go', {
        cancelable: true,
        detail: {pathname, search, hash}
    }));
}

export default function Flow() {
    const ref = useRef<HTMLOutputElement>(null);
    const {pathname, search, hash} = useLocation();
    const navigate = useNavigate();

    navigation = navigate;
    useEffect(() => {

        window.document.addEventListener('click', vaadinRouterGlobalClickHandler);
        window.addEventListener('vaadin-router-go', navigateEventHandler);

        if(popstateListener) {
            // If we have gotten the router popstate handle that
            window.removeEventListener('popstate', popstateListener.listener, popstateListener.useCapture);
            window.addEventListener('popstate', popstateHandler);
        } else {
            // @ts-ignore
            let popstateListeners = window.getEventListeners('popstate');
            if (popstateListeners.length > 0) {
                // pick the first popstate and use that in future.

                popstateListener = popstateListeners[0];
                window.removeEventListener('popstate', popstateListener.listener, popstateListener.useCapture);
                window.addEventListener('popstate', popstateHandler);
            }
        }
        flow.serverSideRoutes[0].action({pathname, search}).then((container) => {
            // Update last navigation when coming into serverside as we might come
            // in using the forward/back buttons.
            lastNavigation = pathname;
            lastNavigationSearch = search;
            const outlet = ref.current?.parentNode;
            if (outlet && outlet !== container.parentNode) {
                outlet.append(container);
            }
            mountedContainer = container;
            if (container.onBeforeEnter) {
                container.onBeforeEnter(
                    {pathname, search},
                    {
                        prevent() {},
                        redirect: navigate
                    },
                    router,
                );
            }
        });
        return () => {
            window.document.removeEventListener('click', vaadinRouterGlobalClickHandler);
            window.removeEventListener('vaadin-router-go', navigateEventHandler);
            if (popstateListener) {
                window.removeEventListener('popstate', popstateHandler);
                window.addEventListener('popstate', popstateListener.listener, popstateListener.useCapture);
            }

            let matched = matchRoutes(Array.from(routes), window.location.pathname);

            // if router force navigated using 'Link' we will need to remove
            // flow from the view
            // If we are going to a non Flow view then we need to clean the Flow
            // view from the dom as we will not be getting a uidl response.
            // @ts-ignore
            if(matched && matched.filter(path => path.route?.element?.type?.name === Flow.name).length == 0) {
                mountedContainer?.parentNode?.removeChild(mountedContainer);
                mountedContainer = undefined;
            }
        };
    }, [pathname, search, hash]);
    return <output ref={ref} />;
}

export const serverSideRoutes = [
    { path: '/*', element: <Flow/> },
];

(function () {
    "use strict";
    [Document, Window].forEach((cst) => {
        if (typeof cst === "function") {
            const eventListenerList = {};
            function pushEventListener(type: string, listener: EventListener,
                                       useCapture: boolean = false) {
                // @ts-ignore
                if (!eventListenerList[type]) {
                    // @ts-ignore
                    eventListenerList[type] = [];
                }
                // @ts-ignore
                eventListenerList[type].push({ type, listener, useCapture });
            }
            function removeEventListener(type: string, listener: EventListener,
                                         useCapture: boolean = false) {
                // @ts-ignore
                if (!eventListenerList[type]) {
                    return;
                }
                // Find the event in the list, If a listener is registered twice, one
                // with capture and one without, remove each one separately. Removal of
                // a capturing listener does not affect a non-capturing version of the
                // same listener, and vice versa.
                // @ts-ignore
                for (let i = 0; i < eventListenerList[type].length; i++) {
                    if (
                        // @ts-ignore
                        eventListenerList[type][i].listener === listener &&
                        // @ts-ignore
                        eventListenerList[type][i].useCapture === useCapture
                    ) {
                        // @ts-ignore
                        eventListenerList[type].splice(i, 1);
                        break;
                    }
                }
                // if no more events of the removed event type are left,remove the group
                // @ts-ignore
                if (eventListenerList[type].length == 0) {
                    // @ts-ignore
                    delete eventListenerList[type];
                }
            }
            function getEventListener(type: string) {
                if (!eventListenerList) return [];
                if (type == undefined) return eventListenerList;
                // @ts-ignore
                if(eventListenerList[type] == undefined) return [];
                // @ts-ignore
                return eventListenerList[type];
            }

            // save the original methods before overwriting them
            const originalAddEventListener = cst.prototype.addEventListener;
            const originalRemoveEventListener = cst.prototype.removeEventListener;

            cst.prototype.addEventListener = function (type: string, listener: EventListener,
                                                       useCapture: boolean = false) {
                originalAddEventListener.call(this, type, listener, useCapture);
                pushEventListener(type, listener, useCapture);
            };

            cst.prototype.removeEventListener = function (type: string, listener: EventListener,
                                                          useCapture: boolean = false) {
                originalRemoveEventListener.call(this, type, listener, useCapture);
                removeEventListener(type, listener, useCapture);
            };

            // @ts-ignore
            cst.prototype.getEventListeners = function (type: string) {
                return getEventListener(type);
            };
        }
    });
})();


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
