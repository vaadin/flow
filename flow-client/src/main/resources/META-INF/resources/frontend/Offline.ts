/* tslint:disable:max-classes-per-file */
import { DBSchema, IDBPDatabase, openDB } from 'idb';
import { EndpointCallMetaInfo } from './Connect';

const VAADIN_DEFERRED_CALL_QUEUE_DB_NAME = 'vaadin-deferred-call-queue';
const VAADIN_DEFERRED_CALL_STORE_NAME = 'deferredCalls';

/**
 * The helper class for the offline features
 */
export class OfflineHelper {

  async storeDeferredCall(deferredCall: DeferredCall): Promise<DeferrableResult<any>> {
    const db = await this.openOrCreateDB();
    const id = await db.add(VAADIN_DEFERRED_CALL_STORE_NAME, deferredCall);
    db.close();
    deferredCall.id = id;
    return { isDeferred: true, deferredCall };
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
            const deferredCall = new DeferredCallSubmitter(request, submitFunction);
            await deferredCallHandler.handleDeferredCallSubmission(deferredCall);
            shouldDeleteTheCall = !deferredCall._shouldKeepDeferredCallInTheQueue()
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

/**
 * The handler for handling submission of deferred endpoint calls. It is called when 
 * Vaadin tries to submit a deferred endpoint call. You can use it e.g. to show
 * notifications to the user when the submission is started, succeeded, or failed.
 * 
 * You can register a <code>DeferredCallHandler</code> to <code>ConnectClient</code>
 * by setting the `deferredCallHandler` property of an existing `ConnectClient` instance
 * <code>
 * client.deferredCallHandler = {
 *  async handleDeferredCallSubmission(submittableDeferredCall: SubmittableDeferredCall) {
 *    
 *  }
 * }
 * <code>
 * or as a `ConnectClient` constructor option when creating a new one.
 * <code>
 * const client = new ConnectClient({deferredCallHandler});
 * </code>
 */
export interface DeferredCallHandler {
  handleDeferredCallSubmission: (deferredCallSubmitter: DeferredCallSubmitter) => Promise<void>;
}

/**
 * An object with an endpoint call meta data. It's saved into IndexedDB when the 
 * endpoint call is deferred so that it can be resubmitted later.
 */
export interface DeferredCall extends EndpointCallMetaInfo{
  /**
   * The id of the DeferredCall in IndexDB
   */
  id?: number;

  /**
   * Optional field indicating if a DeferredCall is being submitted.
   * Intended for internal use, to prevent simultaneous submissions 
   * from multiple browser tabs.
   */
  submitting?: boolean
}


export interface DeferrableResult<T> {
  /**
   * Indicates if the deferrable endpoint call has been deferred
   */
  isDeferred: boolean;

  /**
   * The deferred endpoint call in case the endpoint call is deferred,
   * otherwise undefined.
   */
  deferredCall?: DeferredCall;

  /**
   * The actual result in case the endopoint call is not deferred, 
   * otherwise undefined.
   */
  result?: T;
}

/**
 * A class that can submit a deferred endpoint call, 
 * i.e. has the <code>submit()</code> method. Intended to 
 * be used in <code>DeferredCallHandler</code> for a 
 * fine-tuned control over the deferred call submission.
 */
export class DeferredCallSubmitter {
  private deferredCall: DeferredCall
  private _keepDeferredCallInTheQueue = false;
  private _submitFunction: DeferredCallSubmitFn;
  constructor(endpointCall: DeferredCall, submitFunction: DeferredCallSubmitFn) {
    this.deferredCall = endpointCall;
    this._submitFunction = submitFunction;
  }

  /**
   * Submit the deferred endpoint call.
   */
  async submit(): Promise<any> {
    return this._submitFunction(this.deferredCall.endpoint, this.deferredCall.method, this.deferredCall.params);
  }

  /**
   * By default, if no error is detected when submitting a deferred call, the call will be removed
   * from the IndexedDB. Call this method to keep the deferred endpoint call in the IndexedDB.
   */
  keepDeferredCallInTheQueue() {
    this._keepDeferredCallInTheQueue = true;
  }

  _shouldKeepDeferredCallInTheQueue() {
    return this._keepDeferredCallInTheQueue;
  }
}

interface DeferredCallQueueDB extends DBSchema {
  deferredCalls: {
    value: DeferredCall;
    key: number;
  };
}
