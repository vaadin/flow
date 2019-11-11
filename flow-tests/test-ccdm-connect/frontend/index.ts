import {Router} from '@vaadin/router';
import {Flow} from '@vaadin/flow-frontend/Flow';

const router = new Router(document.querySelector('body'));
const {serverSideRoutes} = new Flow({
  imports: () => import('../target/frontend/generated-flow-imports')
});

router.setRoutes([
  // client-side views
  {
    path: '/login',
    component: 'login-view'
  },
  // pass all unmatched paths to server-side
  ...serverSideRoutes
]);