const webcomponentGlobalCss = {
  css: [],
  importers: []
};

export const injectGlobalWebcomponentCss = (css) => {
  webcomponentGlobalCss.css.push(css);
  webcomponentGlobalCss.importers.forEach(registrar => {
    registrar(css);
  });
};

export const webcomponentGlobalCssInjector = (registrar) => {
  const registeredCss = [];
  const wrapper = (css) => {
    const hash = getHash(css);
    if (!registeredCss.includes(hash)) {
      registeredCss.push(hash);
      registrar(css);
    }
  };
  webcomponentGlobalCss.importers.push(wrapper);
  webcomponentGlobalCss.css.forEach(wrapper);
};