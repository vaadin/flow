# Testing rules

## Write tests first

Always create proper tests for what should work first. If the tests expose
problems in the implementation, fix the implementation after the tests have
been created — don't change the test to match a buggy implementation.

## Keep unit tests minimal

When adding unit tests, add only the essential ones and not more than that.

## Focus when improving tests

When improving existing tests, focus on:

- Verifying actual behavior rather than just "not null".
- Testing JSON structure and content for serialization.
- Adding comprehensive edge case coverage.

## Debugging failing integration tests

When an IT fails, use Playwright to debug the browser behavior and
understand what's actually happening in the UI. Don't guess at the cause
from the test output alone.
