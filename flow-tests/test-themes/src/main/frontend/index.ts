import { Flow } from 'Frontend/generated/jar-resources/Flow';
import { Router } from '@vaadin/router';

const { serverSideRoutes } = new Flow({
  imports: () => import('Frontend/generated/flow/generated-flow-imports.js')
});

const routes = [
  // for client-side, place routes below (more info https://hilla.dev/docs/lit/guides/routing#initializing-the-router)
  {
    path: 'hello',
    component: 'hello-world-view',
    action: async () => {
      await import('./typescript/hello-world-view');
    }
  },
  // for server-side, the next magic line sends all unmatched routes:
  ...serverSideRoutes // IMPORTANT: this must be the last entry in the array
];
export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
