/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { Router } from '@vaadin/router';

const router = new Router(document.querySelector('#outlet'));

router.setRoutes([
  {
    path: '',
    component: 'home-view',
    async action() {
      await import('./home-view.js');
    }
  },
  {
    path: 'about',
    component: 'about-view',
    async action() {
      await import('./about-view.js');
    }
  },
]);
