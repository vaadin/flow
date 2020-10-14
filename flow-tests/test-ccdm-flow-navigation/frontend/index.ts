import {Flow} from '@vaadin/flow-frontend/Flow';
import {Router} from '@vaadin/router';

const {serverSideRoutes} = new Flow({
    imports: () => import('../target/frontend/generated-flow-imports'),
});

const routes = [
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
    // for server-side, the next magic line sends all unmatched routes:
    ...serverSideRoutes // IMPORTANT: this must be the last entry in the array
];

export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
