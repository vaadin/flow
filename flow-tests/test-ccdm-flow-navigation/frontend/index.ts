import {Flow} from '@vaadin/flow-frontend/Flow';
import {Router} from '@vaadin/router';
import {getConnectionIndicator} from '@vaadin/flow-frontend/ConnectionIndicator';

const {serverSideRoutes} = new Flow({
    imports: () => import('../target/frontend/generated-flow-imports'),
});

const routes = [
    {
        path: '',
        component: 'main-view',
        action: async () => { await import ('./views/main/main-view'); },
        children: [
            {
                path: '',
                component: 'about-view',
                action: async () => {
                    await import ('./views/about/about-view');
                }
            },
            {
                path: 'about',
                component: 'about-view',
                action: async () => {
                    await import ('./views/about/about-view');
                }
            },
            {
                path: 'another',
                component: 'another-view',
                action: async () => {
                    await import ('./views/another/another-view');
                }
            },
            // for server-side, the next magic line sends all unmatched routes:
            ...serverSideRoutes // IMPORTANT: this must be the last entry in the array
        ]
    }
];

export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);

// customize the connection indicator
getConnectionIndicator().onlineText = 'Custom online';
getConnectionIndicator().offlineText = 'Custom offline';
getConnectionIndicator().reconnectingText = 'Custom reconnecting';
