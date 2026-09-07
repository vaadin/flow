### Dependencies for tests

If there is a need to add or remove a dependency from the test module
the library should be also be added/removed from the `base(String warName)` 
method in `ArchiveProvider`.

If the tomee execution doesn't return any clear indication on a failure 
except 404 for a module then running with the wildfly profile will often
yield better exception information.
