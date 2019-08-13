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

document.getElementById("button3").addEventListener('click', async e => {
    const view = await flow.navigate({path: 'my/foo view'});
    const div = document.createElement('div');
    div.id = 'div3';
    div.appendChild(view);
    document.body.appendChild(div);
});

