<<<<<<<< HEAD:flow-client/src/test/frontend/client/flow/reactive/CountingComputation.ts
import { Computation } from '../../../../../main/frontend/internal/client/flow/reactive/Computation';
========
import { Computation } from '../../../../main/frontend/internal/reactive/Computation';
>>>>>>>> 6a9bb40c361 (refactor(flow-client): mirror module directory layout in test files):flow-client/src/test/frontend/internal/reactive/CountingComputation.ts

// Mirrors the Java CountingComputation helper: counts recomputations and runs a
// reader on each.
export function countingComputation(reader: () => void): { computation: Computation; getCount: () => number } {
  let count = 0;
  const computation = new Computation(() => {
    count++;
    reader();
  });
  return { computation, getCount: () => count };
}
