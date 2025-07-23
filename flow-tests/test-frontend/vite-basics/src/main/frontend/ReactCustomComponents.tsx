export default function ReactCustomComponents(){
    return (
        <div>
            <span></span>
            <ButtonInCustomComponent />
            <InputInCustomComponent></InputInCustomComponent>
            <div id="response"></div>
        </div>
    );
}


function ButtonInCustomComponent(){
    return <button></button>;
}
function InputInCustomComponent(){
    return <input></input>;
}

// @ts-ignore
(window as any).META_HOT = import.meta.hot;
// @ts-ignore
import.meta.hot.on('vaadin-copilot:analyze-component-response', (payload) => {
    // Execute promise here
    const responseElement = document.body.querySelector('#response');
    responseElement!.innerHTML = JSON.stringify(payload);
    responseElement!.toggleAttribute('response-received', true);
});