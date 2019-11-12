import {Router} from '@vaadin/router';
import {Flow} from '@vaadin/flow-frontend/Flow';

const router = new Router(document.querySelector('#outlet'));
const {serverSideRoutes} = new Flow({
  imports: () => import('../target/frontend/generated-flow-imports')
});

router.setRoutes([
  // client-side views
  {
    path: '/login',
    component: 'login-view',
    action: () => {
      import(/* webpackChunkName: "login-view" */ './login-view');
    }
  },
  // pass all unmatched paths to server-side
  ...serverSideRoutes
]);
