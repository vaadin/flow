import { Flow } from '@vaadin/flow-frontend/Flow';

document.getElementById("button1").addEventListener('click', async e => {
    await import('./another-bundle.js');
    const div = document.createElement('div');
    div.id = 'div1';
    div.textContent = window.anotherBundle;
    document.body.appendChild(div);
});

const flow = new Flow({
    imports: () => import('../target/frontend/generated-flow-imports')
});

document.getElementById("button2").addEventListener('click', async e => {
    await flow.flowInit(true);
    const bootstrapLoaded = !!window.Vaadin.Flow.initApplication;
    const clientLoaded = !!window.Vaadin.Flow.clients.foo.resolveUri;
    const remoteMethod = !!document.body.$server.connectClient;
    const div = document.createElement('div');
    div.id = 'div2';
    div.textContent = bootstrapLoaded + " " + clientLoaded + " " + remoteMethod;
    document.body.appendChild(div);
});

document.getElementById('button3').addEventListener('click', async e => {
    const pathname = document.getElementById('pathname').value;
    // We don't want to depend on `vaadin-router`, thus we do its work here
    // 1. Call action to get the container
    const view = await flow.serverSideRoutes[0].action({pathname: pathname});
    // 2. Call event to ask server to put content in the container
    await view.onBeforeEnter({pathname: pathname}, {prevent: () => {}});
    // 3. Take the router outlet in the page and empty it
    const outlet = document.getElementById('div3');
    outlet.innerHTML = '';
    // 4. Append server side response to the outlet
    const result = document.createElement('result');
    result.id = 'result';
    result.appendChild(view);
    outlet.appendChild(result);
});

document.getElementById("loadVaadinRouter").addEventListener('click', async(e) => {
    const clientRouter = await import('./client-router.js');
    clientRouter.loadRouter(flow);
});

