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
import {Flow as _Flow} from "Frontend/generated/jar-resources/Flow.js";
import {useEffect, useRef,} from "react";
import {useLocation, useNavigate} from "react-router-dom";

const flow = new _Flow({
    imports: () => import("Frontend/generated/flow/generated-flow-imports.js")
});

const router = {
    render() {
        return Promise.resolve();
    }
};

export default function Flow() {
    const ref = useRef<HTMLOutputElement>(null);
    const {pathname, search, hash} = useLocation();
    const navigate = useNavigate();
    useEffect(() => {
        const route = flow.serverSideRoutes[0];
        let mountedContainer: Awaited<ReturnType<typeof route["action"]>> | undefined = undefined;
        route.action({pathname, search}).then((container) => {
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
        };
    }, [pathname, search, hash]);
    return <output ref={ref} />;
}

export const serverSideRoutes = [
    { path: '/*', element: <Flow/> },
];
