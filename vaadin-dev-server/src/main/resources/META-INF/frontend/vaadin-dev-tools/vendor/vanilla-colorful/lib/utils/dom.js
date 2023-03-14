const cache = {};
export const tpl = (html) => {
    let template = cache[html];
    if (!template) {
        template = document.createElement('template');
        template.innerHTML = html;
        cache[html] = template;
    }
    return template;
};
export const fire = (target, type, detail) => {
    target.dispatchEvent(new CustomEvent(type, {
        bubbles: true,
        detail
    }));
};
//# sourceMappingURL=dom.js.map