<<<<<<<< HEAD:flow-client/src/test/frontend/client/flow/reactive/TestReactiveEventRouter.ts
import { ReactiveEventRouter } from '../../../../../main/frontend/internal/client/flow/reactive/ReactiveEventRouter';
import { ReactiveValueChangeEvent } from '../../../../../main/frontend/internal/client/flow/reactive/ReactiveValueChangeEvent';
import type { ReactiveValue } from '../../../../../main/frontend/internal/client/flow/reactive/ReactiveValue';
import type { ReactiveValueChangeListener } from '../../../../../main/frontend/internal/client/flow/reactive/ReactiveValueChangeListener';
========
import { ReactiveEventRouter } from '../../../../main/frontend/internal/reactive/ReactiveEventRouter';
import { ReactiveValueChangeEvent } from '../../../../main/frontend/internal/reactive/ReactiveValueChangeEvent';
import type { ReactiveValue } from '../../../../main/frontend/internal/reactive/ReactiveValue';
import type { ReactiveValueChangeListener } from '../../../../main/frontend/internal/reactive/ReactiveValueChangeListener';
>>>>>>>> 6a9bb40c361 (refactor(flow-client): mirror module directory layout in test files):flow-client/src/test/frontend/internal/reactive/TestReactiveEventRouter.ts

// Mirrors the Java TestReactiveEventRouter test helper: a router whose source
// routes change registrations back to itself, with an invalidate() that fires a
// change event.
export class TestReactiveEventRouter extends ReactiveEventRouter<
  ReactiveValueChangeListener,
  ReactiveValueChangeEvent
> {
  constructor() {
    const source: ReactiveValue = {
      addReactiveValueChangeListener: () => {
        throw new Error('event source not wired yet');
      }
    };
    super(
      source,
      (l) => l,
      (l, e) => l(e)
    );
    source.addReactiveValueChangeListener = (l) => this.addReactiveListener(l);
  }

  invalidate(): void {
    this.fireEvent(new ReactiveValueChangeEvent(this.getReactiveValue()));
  }
}
