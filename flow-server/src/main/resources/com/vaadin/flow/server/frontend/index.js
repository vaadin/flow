import { Flow } from '@vaadin/flow-frontend/Flow';

const flow = new Flow({
  imports: () => import('[to-be-generated-by-flow]')
});
// Let the flow-server control the application
flow.start();
