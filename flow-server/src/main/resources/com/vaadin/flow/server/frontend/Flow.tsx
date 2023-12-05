/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import { useEffect, useRef } from "react";
import { NavigateFunction, useLocation, useNavigate } from "react-router-dom";

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
        return;
    }

    // ignore the click if the target is external to the app
    // In IE11 HTMLAnchorElement does not have the `origin` property
    const origin = anchor.origin || getAnchorOrigin(anchor);
    if (origin !== window.location.origin) {
        return;
    }

    // if none of the above, convert the click into a navigation event
    const {pathname, search, hash} = anchor;
    if (fireRouterEvent('go', {pathname, search, hash})) {
        event.preventDefault();
        // for a click event, the scroll is reset to the top position.
        if (event && event.type === 'click') {
            window.scrollTo(0, 0);
        }
    }
}

// We can't initiate useNavigate() from outside React component so we store it here for use in the navigateEvent.
let navigation: NavigateFunction | ((arg0: any, arg1: { replace: boolean; }) => void);
let mountedContainer: Awaited<ReturnType<typeof flow.serverSideRoutes[0]["action"]>> | undefined = undefined;

// @ts-ignore
function navigateEventHandler(event) {
    if (event && event.preventDefault) {
        event.preventDefault();
    }

    if (mountedContainer?.onBeforeLeave) {
        mountedContainer?.onBeforeLeave({pathname: event.detail.pathname, search: event.detail.search}, {
            prevent() {},
        }, router);
    } else {
        navigation(event.detail.pathname, {replace: false})
    }
}

export default function Flow() {
    const ref = useRef<HTMLOutputElement>(null);
    const {pathname, search, hash} = useLocation();
    const navigate = useNavigate();
    navigation = navigate;
    useEffect(() => {
        window.document.addEventListener('click', vaadinRouterGlobalClickHandler);
        window.addEventListener('vaadin-router-go', navigateEventHandler);
        flow.serverSideRoutes[0].action({pathname, search}).then((container) => {
            const outlet = ref.current?.parentNode;
            if (outlet && outlet !== container.parentNode) {
                outlet.insertBefore(container, ref.current);
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
            if (mountedContainer?.onBeforeLeave) {
                mountedContainer?.onBeforeLeave({pathname, search}, {
                    prevent() {},
                }, router);
            }
            mountedContainer?.parentNode?.removeChild(mountedContainer);
            mountedContainer = undefined;
            window.document.removeEventListener('click', vaadinRouterGlobalClickHandler);
            window.removeEventListener('vaadin-router-go', navigateEventHandler);
        };
    }, [pathname, search, hash]);
    return <output ref={ref} />;
}

export const serverSideRoutes = [
    { path: '/*', element: <Flow/> },
];
