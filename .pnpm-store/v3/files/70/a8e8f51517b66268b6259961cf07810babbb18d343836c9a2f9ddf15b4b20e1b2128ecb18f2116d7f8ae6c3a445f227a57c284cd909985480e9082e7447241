/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { FlattenedNodesObserver } from '@polymer/polymer/lib/utils/flattened-nodes-observer.js';
import { GridTemplatizer } from './template-renderer-grid-templatizer.js';
import { Templatizer } from './template-renderer-templatizer.js';

function createRenderer(component, template, TemplatizerClass = Templatizer) {
  const templatizer = TemplatizerClass.create(component, template);

  const renderer = (root, _owner, model) => {
    templatizer.render(root, model);
  };

  template.__templatizer = templatizer;
  renderer.__templatized = true;

  return renderer;
}

function assignRenderer(component, rendererName, renderer) {
  const oldRenderer = component[rendererName];

  if (oldRenderer && !oldRenderer.__templatized) {
    const tag = component.localName;

    throw new Error(`Cannot use both a template and a renderer for <${tag} />.`);
  }

  component[rendererName] = renderer;
}

function showTemplateWarning(component) {
  if (component.__suppressTemplateWarning) {
    return;
  }

  if (component.hasAttribute('suppress-template-warning')) {
    return;
  }

  console.warn(
    `WARNING: <template> inside <${component.localName}> is deprecated. Use a renderer function instead (see https://vaad.in/template-renderer)`,
  );

  component.__suppressTemplateWarning = true;
}

function processGridTemplate(grid, template) {
  if (template.matches('.row-details')) {
    const renderer = createRenderer(grid, template, GridTemplatizer);
    assignRenderer(grid, 'rowDetailsRenderer', renderer);
  }
}

function processGridColumnTemplate(column, template) {
  if (template.matches('.header')) {
    const renderer = createRenderer(column, template);
    assignRenderer(column, 'headerRenderer', renderer);
    return;
  }

  if (template.matches('.footer')) {
    const renderer = createRenderer(column, template);
    assignRenderer(column, 'footerRenderer', renderer);
    return;
  }

  if (template.matches('.editor')) {
    const renderer = createRenderer(column, template, GridTemplatizer);
    assignRenderer(column, 'editModeRenderer', renderer);
    return;
  }

  const renderer = createRenderer(column, template, GridTemplatizer);
  assignRenderer(column, 'renderer', renderer);
}

function processTemplate(component, template) {
  showTemplateWarning(component);

  if (component.__gridElement) {
    processGridTemplate(component, template);
    return;
  }

  if (component.__gridColumnElement) {
    processGridColumnTemplate(component, template);
    return;
  }

  const renderer = createRenderer(component, template);
  assignRenderer(component, 'renderer', renderer);
}

function processTemplates(component) {
  FlattenedNodesObserver.getFlattenedNodes(component)
    .filter((child) => {
      return child instanceof HTMLTemplateElement;
    })
    .forEach((template) => {
      // Ignore templates which have been processed earlier
      if (template.__templatizer) {
        return;
      }

      processTemplate(component, template);
    });
}

function observeTemplates(component) {
  if (component.__templateObserver) {
    return;
  }

  component.__templateObserver = new FlattenedNodesObserver(component, () => {
    processTemplates(component);
  });
}

/**
 * Public API
 */
if (!window.Vaadin) {
  window.Vaadin = {};
}

window.Vaadin.templateRendererCallback = (component) => {
  processTemplates(component);
  observeTemplates(component);
};
