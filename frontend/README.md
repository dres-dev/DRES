# DRES Frontend

This is the frontend / UI code for DRES - the Distributed Retrieval Evaluation Server - written in Angular. The UI requires a running DRES backend.

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 9.1.0.

## Configuration Customisation

It is possible to customise the configuration **before** deoploying.
The main reason for that is enabling TLS.

Please modify the `src/config.json` accordingly and package
the frontend using the corresponding gradle command:

```
./gradlew packageFrontend -DincludeConfig
```

The config is structured as follows:

```json
{
  "endpoint": {
    "host": "localhost", // When serving the frontend elsewhere, specify the backend host
    "port": 8080, // When serving the frontend elsewhere, specify the backend port
    "tls": false // Set to `true` when TLS should be enabled. Requires backend config with matching `"enableSsl":true`
  },
  "effects": {
    "mute": false // Whether to globally mute audio (fx and video)
  }
}
```

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

As described in this [help](https://support.google.com/chrome/thread/26291731?hl=en),
developers using Chrome (version > 80) have to disable the somewhat new `Same-Site` cookie policy.
In a new tab set both, `chrome://flags/#cookies-without-same-site-must-be-secure` and `chrome://flags/#same-site-by-default-cookies`, to _disabled_.
Do not forget to set them to _default_, once development has stopped.
Alternatively one can

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

In order to update / generate the Open API stubs and data model, run the following command while the DRES backend is running

`openapi-generator generate -g typescript-angular -i http://localhost:8080/swagger-docs -o openapi --skip-validate-spec --additional-properties npmName=@dres-openapi/api,snapshot=true,ngVersion=9.1.0`

The assumption for this snippet is, that the DRES backend is running on localhost using port 8080. Adjust according to your needs.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Development

This project uses [yarn](https://yarnpkg.com/) for development.
For developers who start working on this project the very first time
prepare your dev environment by running `yarn install`.

## Linting

We use [prettier](https://prettier.io/) in combination with [eslint](https://eslint.org/) for linting.
[Husky](https://typicode.github.io/husky/#/) is used for version control hooks.
Due to the repo structure, it's mandatory that husky hooks first move tho this directory
(`cd frontend/`) before running the command.
