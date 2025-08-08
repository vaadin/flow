/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import type { Plugin } from 'vite';
import { FEATURES, COMPONENT_ANALYZER_REQ_KEY, COMPONENT_ANALYZER_RES_KEY } from './consts';
import type { GenericResponse } from './utils';
import { getNodesInfosInFiles } from './component-analyzer';

/**
 * The plugin creates API and transform code based on the features available in Vaadin Copilot.
 *
 * @constructor
 */

export default function vaadinCopilotPlugin(): Plugin {
  return {
    name: 'vaadin-copilot-plugin',
    transform(code, id) {
      const [bareId] = id.split('?');
      if (!bareId.endsWith('.tsx')) {
        return;
      }
      const injectCode = `
        window.Vaadin = window.Vaadin || {};
        window.Vaadin.copilot = window.Vaadin.copilot || {};
        window.Vaadin.copilot.VITE_COPILOT_PLUGIN = window.Vaadin.copilot.VITE_COPILOT_PLUGIN || {};
        window.Vaadin.copilot.VITE_COPILOT_PLUGIN.availableFeatures=[${FEATURES.map((feature) => `"${feature}"`).join(',')}];
      `;
      return {
        code: injectCode + code,
        map: null,
      };
    },
    configureServer(server) {
      server.ws.on(COMPONENT_ANALYZER_REQ_KEY, (data, client) => {
        const response: GenericResponse<Record<string, any>> = {};
        try {
          const filePaths = data.filePaths as string[] | undefined;
          if (!filePaths) {
            response.error = true;
            response.errorMessage = 'No file path is provided';
            client.send(COMPONENT_ANALYZER_RES_KEY, response);
            return;
          }
          response.body = getNodesInfosInFiles(filePaths);
        } catch (e: any) {
          response.error = true;
          response.errorMessage = e;
        }
        client.send(COMPONENT_ANALYZER_RES_KEY, response);
      });
    },
  };
}
