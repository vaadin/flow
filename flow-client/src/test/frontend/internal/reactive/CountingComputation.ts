import { Computation } from '../../../../main/frontend/internal/reactive/Computation';

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
