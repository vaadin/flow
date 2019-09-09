import {Flow} from '@vaadin/flow-frontend/Flow';
import {Router} from '@vaadin/router';

//------- Configure flow
const flow = new Flow({
  imports: () => import('../target/frontend/generated-flow-imports')
});
const createNavigationLink = (text, link) => {
  const aLink = document.createElement('a');
  aLink.href = link;
  aLink.textContent = text;
  return aLink;
}
//------- Configure Router
const routes = [
  {
    path: 'client-view',
    action: () => {
      const div = document.createElement('div');
      div.textContent = 'Client view';
      div.id = 'clientSideView';
      return div;
    }
  },
  flow.route
];

const routerContainer = document.createElement('div');
const navigationContainer = document.createElement('div');
navigationContainer.id = 'navigationContainer';
navigationContainer.appendChild(createNavigationLink('Empty view', ''));
navigationContainer.appendChild(createNavigationLink('Client side view', 'client-view'));
navigationContainer.appendChild(createNavigationLink('Server view', 'serverview'));
navigationContainer.appendChild(createNavigationLink('View with all events', 'view-with-all-events'));
routerContainer.appendChild(navigationContainer);

const outlet = document.createElement('div');
outlet.id = 'outlet';
routerContainer.appendChild(outlet);
document.body.appendChild(routerContainer);
const router = new Router(outlet);

router.setRoutes(routes);


