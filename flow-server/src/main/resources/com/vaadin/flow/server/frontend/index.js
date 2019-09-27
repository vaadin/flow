// import Flow module to enable traditional Vaadin server-side navigation
// If you prefer to use TypeScript, you can import `Flow.ts` instead
import { Flow } from '@vaadin/flow-frontend/Flow.js';

const flow = new Flow({
  imports: () => import('[to-be-generated-by-flow]')
});
// Let the flow-server control the application and use traditional server-side routing
flow.start();
// If you prefer to use client-side routing, please check out the 'Client-side Routing' page in `https://vaadin.com/docs`