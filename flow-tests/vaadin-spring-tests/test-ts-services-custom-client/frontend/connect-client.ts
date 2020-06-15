import {ConnectClient, MiddlewareContext, MiddlewareNext} from '@vaadin/flow-frontend/Connect';

async function logger(context: MiddlewareContext, next: MiddlewareNext): Promise<Response> {
  const start = new Date().getTime();
  try {
    return await next(context);
  } finally {
    const duration = new Date().getTime() - start;
    const message = `[LOG] ${context.endpoint}/${context.method} took ${duration} ms`;
    console.log(message);
    document.querySelector('#log')!.append(message);
  }
}

const client = new ConnectClient({prefix: 'connect', middlewares: [logger]});
export default client;
