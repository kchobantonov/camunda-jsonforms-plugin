# [Camunda](https://camunda.com/) [JSON Forms](https://jsonforms.io/) Plugin

Provides client-side and server-side integration for using Camunda Embedded Forms with JSON Forms using [JSON Forms Vuetify renderers](https://github.com/eclipsesource/jsonforms-vuetify-renderers).

## Docker

### Try it

------

With Camunda Platform 7.18.0:

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile -t camunda-bpm-platform:7.18.0-jsonforms .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.18.0-jsonforms
```

On your browser go to
<http://localhost:8080/camunda>

**Note**: docker image is based on camumda-bpm-platform and will not include the demo project, instead the camunda-invoice application that is shipped with camunda-bpm-platform will be modified using the files under [camunda-invoice](./docker-camunda-bpm-platform/camunda-invoice). You can check the JSON Forms schema, uischema, i18n JSON files that are used fro camunda-invoice demo.

------

With Camunda Platform 7.18.0 and [Minimal "history plugins" for Camunda Cockpit](https://github.com/datakurre/camunda-cockpit-plugins) plugin:

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile-history -t camunda-bpm-platform:7.18.0-jsonforms-history .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.18.0-jsonforms-history
```

On your browser go to
<http://localhost:8080/camunda>
