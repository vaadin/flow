/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {Flow} from 'Frontend/generated/jar-resources/Flow';
import {Router} from '@vaadin/router';

const {serverSideRoutes} = new Flow({
    imports: () => import('../target/frontend/generated-flow-imports'),
});

const routes = [
    // for client-side, place routes below (more info https://vaadin.com/docs/v15/flow/typescript/creating-routes.html)
    {
        path: 'hello',
        component: 'hello-world-view',
        action: async () => {
            await import('./typescript/hello-world-view');
        },
    },
    // for server-side, the next magic line sends all unmatched routes:
    ...serverSideRoutes, // IMPORTANT: this must be the last entry in the array
]
;

export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
