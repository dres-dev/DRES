# DRES Frontend

This is the frontend / UI code for DRES - the Distributed Retrieval Evaluation Server - written in Angular. The UI requires a running DRES backend.

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 9.1.0.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`. 

In order to update / generate the Open API stubs and data model, run the following command while the DRES backend is running

`openapi-generator generate -g typescript-angular -i http://localhost:8080/swagger-docs -o openapi --skip-validate-spec --additional-properties npmName=@dres-openapi/api,snapshot=true,ngVersion=9.1.0`

The assumption for this snippet is, that the DRES backend is running on localhost using port 8080. Adjust according to your needs.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.
