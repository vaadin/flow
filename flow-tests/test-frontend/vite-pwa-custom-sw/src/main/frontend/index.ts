import { Router } from '@vaadin/router';

const router = new Router(document.querySelector('#outlet'));

router.setRoutes([
  {
    path: '',
    component: 'home-view',
    async action() {
      await import('./home-view.js');
    }
  }
]);
