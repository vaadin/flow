// Specific prefix used for both request and responses
export const VAADIN_COPILOT_PLUGIN_PREFIX_KEY = 'vaadin-copilot:';

export const COMPONENT_ANALYZER_REQ_KEY = `${VAADIN_COPILOT_PLUGIN_PREFIX_KEY}analyze-component`;
export const COMPONENT_ANALYZER_RES_KEY = `${COMPONENT_ANALYZER_REQ_KEY}-response`;

// Available features implemented in Vite Plugin
export const FEATURES = ['COMPONENT_ANALYZER'];
