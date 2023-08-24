import { Connection } from '../connection';
import { ComponentReference } from '../component-util';

export enum Commands {
  response = 'themeEditorResponse',
  loadComponentMetadata = 'themeEditorComponentMetadata',
  setLocalClassName = 'themeEditorLocalClassName',
  setCssRules = 'themeEditorRules',
  loadRules = 'themeEditorLoadRules',
  history = 'themeEditorHistory',
  openCss = 'themeEditorOpenCss',
  markAsUsed = 'themeEditorMarkAsUsed'
}

export enum ResponseCode {
  ok = 'ok',
  error = 'error'
}

export interface BaseResponse {
  requestId: string;
  code: ResponseCode;
}

export interface LoadComponentMetadataResponse extends BaseResponse {
  accessible?: boolean;
  className?: string;
  suggestedClassName?: string;
}

export interface LoadPreviewResponse extends BaseResponse {
  css: string;
}

export interface ServerCssRule {
  selector: string;
  properties: { [key: string]: string };
}

export interface LoadRulesResponse extends BaseResponse {
  rules: ServerCssRule[];
}

interface RequestHandle {
  resolve: (response: unknown) => void;
  reject: (response: unknown) => void;
}

export class ThemeEditorApi {
  private wrappedConnection: Connection;
  private pendingRequests: { [key: string]: RequestHandle } = {};
  private requestCounter: number = 0;
  private globalUiId: number | undefined;

  constructor(wrappedConnection: Connection) {
    this.wrappedConnection = wrappedConnection;
    const prevOnMessage = this.wrappedConnection.onMessage;
    this.wrappedConnection.onMessage = (message: any) => {
      if (message.command === Commands.response) {
        this.handleResponse(message.data);
      } else {
        prevOnMessage.call(this.wrappedConnection, message);
      }
    };
  }

  private sendRequest(command: string, data: any) {
    const requestId = (this.requestCounter++).toString();
    const uiId = data['uiId'] ?? this.getGlobalUiId();

    return new Promise<any>((resolve, reject) => {
      this.wrappedConnection.send(command, {
        ...data,
        requestId,
        uiId
      });
      this.pendingRequests[requestId] = {
        resolve,
        reject
      };
    });
  }

  private handleResponse(data: BaseResponse) {
    const requestHandle = this.pendingRequests[data.requestId];
    if (!requestHandle) {
      console.warn('Received response for unknown request');
      return;
    }

    delete this.pendingRequests[data.requestId];

    if (data.code === ResponseCode.ok) {
      requestHandle.resolve(data);
    } else {
      requestHandle.reject(data);
    }
  }

  public loadComponentMetadata(componentRef: ComponentReference): Promise<LoadComponentMetadataResponse> {
    return this.sendRequest(Commands.loadComponentMetadata, { nodeId: componentRef.nodeId });
  }

  public setLocalClassName(componentRef: ComponentReference, className: string): Promise<BaseResponse> {
    return this.sendRequest(Commands.setLocalClassName, { nodeId: componentRef.nodeId, className });
  }

  public setCssRules(rules: ServerCssRule[]): Promise<BaseResponse> {
    return this.sendRequest(Commands.setCssRules, { rules });
  }

  public loadRules(selectors: string[]): Promise<LoadRulesResponse> {
    return this.sendRequest(Commands.loadRules, { selectors });
  }

  public markAsUsed(): Promise<BaseResponse> {
    return this.sendRequest(Commands.markAsUsed, {});
  }

  public undo(requestId: string): Promise<BaseResponse> {
    return this.sendRequest(Commands.history, { undo: requestId });
  }

  public redo(requestId: string): Promise<BaseResponse> {
    return this.sendRequest(Commands.history, { redo: requestId });
  }

  public openCss(selector: string): Promise<BaseResponse> {
    return this.sendRequest(Commands.openCss, { selector });
  }

  private getGlobalUiId(): number {
    if (this.globalUiId === undefined) {
      const vaadin = (window as any).Vaadin;
      if (vaadin && vaadin.Flow) {
        const { clients } = vaadin.Flow;
        const appIds = Object.keys(clients);
        for (const appId of appIds) {
          const client = clients[appId];
          if (client.getNodeId) {
            this.globalUiId = client.getUIId();
            break;
          }
        }
      }
    }

    return this.globalUiId ?? -1;
  }
}
