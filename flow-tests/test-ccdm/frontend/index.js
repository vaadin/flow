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
    await flow.start();
    const bootstrapLoaded = !!window.Vaadin.Flow.initApplication;
    const clientLoaded = !!window.Vaadin.Flow.resolveUri;
    const remoteMethod = !!document.body.$server.connectClient;
    const div = document.createElement('div');
    div.id = 'div2';
    div.textContent = bootstrapLoaded + " " + clientLoaded + " " + remoteMethod;
    document.body.appendChild(div);
});

document.getElementById('button3').addEventListener('click', async e => {
    const route = document.getElementById('routeValue').value;
    const view = await flow.route({pathname: route});
    const div = document.getElementById('div3');
    div.innerHTML = '';

    await view.onBeforeEnter({pathname: route}, {prevent: () => {}});

    const result = document.createElement('result');
    result.id = 'result';
    result.appendChild(view);
    div.appendChild(result);
});

