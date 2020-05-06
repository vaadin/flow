# Vaadin Flow Client

This is the client part of flow. It is composed by two parts

   - The flow client protocol, developed in java and compiled with GWT.
   - An npm package that can be used as a JS library to use flow UI in any client app.

## Building the project

`mvn clean package` will do everything, and will leave a `.jar` file
with everything set in the `target` folder.

## Installing the `.jar` 

Run `mvn install` or just `mvn install -DskipTests` 

## Running the npm module

1. Install dependencies

Run either `npm install` or `mvn compile`

2. Compile Code and Run tests

Run either `npm test` or `mvn test`

3. Debug Tests

Run `npm run debug` then point your browser to http://localhost:9000/__intern/ to debug tests.

