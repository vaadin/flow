import { Flow } from 'Frontend/generated/jar-resources/Flow';
import { Router } from '@vaadin/router';
import { connectionIndicator } from '@vaadin/common-frontend';

const { serverSideRoutes } = new Flow({
  imports: () => import('Frontend/generated/flow/generated-flow-imports.js')
});

const routes = [
  {
    path: '',
    component: 'main-view',
    action: async () => {
      await import('./views/main/main-view');
    },
    children: [
      {
        path: '',
        component: 'about-view',
        action: async () => {
          await import('./views/about/about-view');
        }
      },
      {
        path: 'about',
        component: 'about-view',
        action: async () => {
          await import('./views/about/about-view');
        }
      },
      {
        path: 'another',
        component: 'another-view',
        action: async () => {
          await import('./views/another/another-view');
        }
      },
      {
        path: 'deep/another',
        component: 'another-view',
        action: async () => {
          await import('./views/another/another-view');
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
connectionIndicator.onlineText = 'Custom online';
connectionIndicator.offlineText = 'Custom offline';
connectionIndicator.reconnectingText = 'Custom reconnecting';
