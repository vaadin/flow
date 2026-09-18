// @ts-nocheck
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.jsInvokers = window.Vaadin.Flow.jsInvokers || {};
window.Vaadin.Flow.jsInvokers['com.vaadin.base.devserver.hotswap.impl.JsInvokerHotswapperTest$GreeterJs'] =
  Object.assign(
    window.Vaadin.Flow.jsInvokers['com.vaadin.base.devserver.hotswap.impl.JsInvokerHotswapperTest$GreeterJs'] || {},
    {
      'showGreeting/1': async function ($0) {
        window.alert($0);
        this.focus();
      }
    }
  );
if (import.meta.hot) {
  import.meta.hot.accept();
}
export {};
