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

/**
 * Calculate a 32 bit FNV-1a hash
 * Found here: https://gist.github.com/vaiorabbit/5657561
 * Ref.: http://isthe.com/chongo/tech/comp/fnv/
 *
 * @param {string} str the input value
 * @returns {string} 32 bit (as 8 byte hex string)
 */
function hashFnv32a(str) {
  /*jshint bitwise:false */
  let i,
    l,
    hval = 0x811c9dc5;

  for (i = 0, l = str.length; i < l; i++) {
    hval ^= str.charCodeAt(i);
    hval += (hval << 1) + (hval << 4) + (hval << 7) + (hval << 8) + (hval << 24);
  }

  // Convert to 8 digit hex string
  return ('0000000' + (hval >>> 0).toString(16)).substr(-8);
}

/**
 * Calculate a 64 bit hash for the given input.
 * Double hash is used to significantly lower the collision probability.
 *
 * @param {string} input value to get hash for
 * @returns {string} 64 bit (as 16 byte hex string)
 */
function getHash(input) {
  let h1 = hashFnv32a(input); // returns 32 bit (as 8 byte hex string)
  return h1 + hashFnv32a(h1 + input);
}