/* tslint:disable:max-classes-per-file */
import { DBSchema, IDBPDatabase, openDB } from 'idb';

const VAADIN_DEFERRED_CALL_QUEUE_DB_NAME = 'vaadin-deferred-call-queue';
const VAADIN_DEFERRED_CALL_STORE_NAME = 'deferredCalls';

/**
 * The helper class for the offlie featurs
 */
class Offline {
  checkOnline(): boolean {
    return navigator.onLine;
  }

  async cacheEndpointRequest(endpointRequest: DeferredCall): Promise<DeferredCall> {
    const db = await this.openOrCreateDB();
    const id = await db.add(VAADIN_DEFERRED_CALL_STORE_NAME, endpointRequest);
    db.close();
    endpointRequest.id = id;
    return endpointRequest;
  }

  async processDeferredCalls(submitFunction: DeferredCallSubmitFn, deferredCallHandler?: DeferredCallHandler) {
    const db = await this.openOrCreateDB();

    /**
     * Cannot wait for submitting the cached requests in the indexed db transaction,
     * as the transaction only wait for db operations.
     * See https://github.com/jakearchibald/idb#transaction-lifetime
     */
    const shouldSubmit = await this.shouldSubmitCachedRequests(db);

    if (shouldSubmit) {
      await this.submitCachedRequests(db, submitFunction, deferredCallHandler);
    }

    db.close();
  }

  private async openOrCreateDB(): Promise<IDBPDatabase<DeferredCallQueueDB>> {
    return openDB<DeferredCallQueueDB>(VAADIN_DEFERRED_CALL_QUEUE_DB_NAME, 1, {
      upgrade(db) {
        db.createObjectStore(VAADIN_DEFERRED_CALL_STORE_NAME, {
          keyPath: 'id',
          autoIncrement: true
        });
      },
    });
  }

  private async shouldSubmitCachedRequests(db: IDBPDatabase<DeferredCallQueueDB>) {
    let shouldSubmit = false;
    if (db.objectStoreNames.contains(VAADIN_DEFERRED_CALL_STORE_NAME) && await db.count(VAADIN_DEFERRED_CALL_STORE_NAME) > 0) {
      const tx = db.transaction(VAADIN_DEFERRED_CALL_STORE_NAME, 'readwrite');

      let cursor = await tx.store.openCursor();
      while (cursor) {
        const request = cursor.value;
        if (!request.submitting) {
          shouldSubmit = true;
          request.submitting = true;
          cursor.update(request);
        }
        cursor = await cursor.continue();
      }
      await tx.done;
    }
    return shouldSubmit;
  }

  private async submitCachedRequests(db: IDBPDatabase<DeferredCallQueueDB>, submitFunction: DeferredCallSubmitFn, deferredCallHandler?: DeferredCallHandler) {
    const errors: Error[] = [];
    const cachedRequests = await db.getAll(VAADIN_DEFERRED_CALL_STORE_NAME);
    for (const request of cachedRequests) {
      try {
        if (request.submitting) {
          let shouldDeleteTheCall = true;
          if (deferredCallHandler) {
            const deferredCall = new SubmittableDeferredCall(request, submitFunction);
            await deferredCallHandler.handleDeferredCall(deferredCall);
            shouldDeleteTheCall = !deferredCall._shouldKeepInTheQueue()
          } else {
            await submitFunction(request.endpoint, request.method, request.params);
          }
          if (shouldDeleteTheCall) {
            await db.delete(VAADIN_DEFERRED_CALL_STORE_NAME, request.id!);
          }
        }
      } catch (error) {
        errors.push(error);
      }
    }

    await this.resetSubmittingStatusForRemainingDeferredCalls(db);

    if (errors.length > 0) {
      throw errors;
    }
  }

  private async resetSubmittingStatusForRemainingDeferredCalls(db: IDBPDatabase<DeferredCallQueueDB>) {
    const remainingRequests = await db.getAll(VAADIN_DEFERRED_CALL_STORE_NAME);
    for (const request of remainingRequests) {
      request.submitting = false;
      await db.put(VAADIN_DEFERRED_CALL_STORE_NAME, request);
    }
  }
}

type DeferredCallSubmitFn = (
  endpoint: string,
  method: string,
  params?: any
) => Promise<any>;

export interface DeferredCallHandler {
  handleDeferredCall: (deferrableCall: SubmittableDeferredCall) => Promise<void>;
}

export interface DeferredCall {
  id?: number;
  endpoint: string;
  method: string;
  params?: any;
  submitting?: boolean
}

export interface DeferrableResult<T> {
  isDeferred: boolean;
  deferredCall?: DeferredCall;
  result?: T;
}

export class SubmittableDeferredCall implements DeferredCall {
  id?: number;
  endpoint: string;
  method: string;
  params?: any;
  submitting?: boolean;
  private _keepInTheQueue = false;
  private _submitFunction: DeferredCallSubmitFn;
  constructor(endpointCall: DeferredCall, submitFunction: DeferredCallSubmitFn) {
    this.id = endpointCall.id;
    this.endpoint = endpointCall.endpoint;
    this.method = endpointCall.method;
    this.params = endpointCall.params;
    this._submitFunction = submitFunction;
  }

  async submit(): Promise<any> {
    return this._submitFunction(this.endpoint, this.method, this.params);
  }

  keepInTheQueue() {
    this._keepInTheQueue = true;
  }

  _shouldKeepInTheQueue() {
    return this._keepInTheQueue;
  }
}

interface DeferredCallQueueDB extends DBSchema {
  deferredCalls: {
    value: DeferredCall;
    key: number;
  };
}

export const offline = new Offline();