interface Vaadin {
  developmentModeCallback?: {
    'usage-statistics'?(): void;
  };
  registrations?: Array<{ is: string; version: string }>;
  usageStatsChecker?: {
    maybeGatherAndSend(): void;
  };
  featureFlags?: Record<string, boolean>;
}

interface Window {
  Vaadin: Vaadin;
}
