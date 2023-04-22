import { ThemeEditorApi } from './api';

type HistoryCustomizerFn = () => void;

export interface HistoryEntry {
  requestId: string;
  execute?: HistoryCustomizerFn;
  rollback?: HistoryCustomizerFn;
}

export interface ThemeEditorHistoryActions {
  allowUndo: boolean;
  allowRedo: boolean;
}

// Keep history state as singleton, so that recreating theme editor
// (e.g. switching between dev tool tabs) doesn't reset it
const state = {
  index: -1,
  entries: [] as HistoryEntry[]
};

export class ThemeEditorHistory {
  private api: ThemeEditorApi;

  constructor(api: ThemeEditorApi) {
    this.api = api;
  }

  get allowUndo() {
    return state.index >= 0;
  }

  get allowRedo() {
    return state.index < state.entries.length - 1;
  }

  get allowedActions(): ThemeEditorHistoryActions {
    return {
      allowUndo: this.allowUndo,
      allowRedo: this.allowRedo
    };
  }

  push(requestId: string, execute?: HistoryCustomizerFn, rollback?: HistoryCustomizerFn): ThemeEditorHistoryActions {
    const entry = {
      requestId,
      execute,
      rollback
    };
    state.index++;
    state.entries = state.entries.slice(0, state.index);
    state.entries.push(entry);

    if (execute) {
      try {
        execute();
      } catch (error) {
        console.error('Execute history entry failed', error);
      }
    }

    return this.allowedActions;
  }

  async undo(): Promise<ThemeEditorHistoryActions> {
    if (!this.allowUndo) {
      return this.allowedActions;
    }
    const entry = state.entries[state.index];
    state.index--;

    try {
      await this.api.undo(entry.requestId);
      if (entry.rollback) {
        entry.rollback();
      }
    } catch (error) {
      console.error('Undo failed', error);
    }
    return this.allowedActions;
  }

  async redo(): Promise<ThemeEditorHistoryActions> {
    if (!this.allowRedo) {
      return this.allowedActions;
    }
    state.index++;
    const entry = state.entries[state.index];

    try {
      await this.api.redo(entry.requestId);
      if (entry.execute) {
        entry.execute();
      }
    } catch (error) {
      console.error('Redo failed', error);
    }
    return this.allowedActions;
  }

  // Only intended to be used for testing
  static clear() {
    state.entries = [];
    state.index = -1;
  }
}
