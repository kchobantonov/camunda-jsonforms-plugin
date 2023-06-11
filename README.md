# [Camunda](https://camunda.com/) [JSON Forms](https://jsonforms.io/) Plugin

Provides client-side and server-side integration for using Camunda Embedded Forms with JSON Forms using [JSON Forms Vuetify renderers](https://github.com/eclipsesource/jsonforms-vuetify-renderers).

## Developers Documentation

### First time setup

* Install [Java 1.8 or later](https://www.java.com/en/download/help/download_options.html)
* Install [Maven 3.6](https://maven.apache.org/install.html)
* Clone this repository

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin
```

### Build & Testing

```bash
mvn clean install
```

### Run Demo project

```bash
cd demo
mvn spring-boot:run
```

### Rebuild package-lock.json

```bash
cd plugin-ui
rm -f package-lock.json
docker run --rm -v $(pwd):/usr/src/app -w /usr/src/app node:14.21.3 sh -c "npm install && npm run init"
```


### Docker

**Note**: The docker image is based on camumda-bpm-platform and will not include the demo project, instead the camunda-invoice application that is shipped with camunda-bpm-platform will be modified using the files under [camunda-invoice](./docker-camunda-bpm-platform/camunda-invoice). You can check the JSON Forms schema, uischema, i18n JSON files that are used for camunda-invoice demo under that folder.

---

* With Camunda Platform 7.18.0

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile -t camunda-bpm-platform:7.18.0-jsonforms .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.18.0-jsonforms
```

Open <http://localhost:8080/camunda>

---

* With Camunda Platform 7.18.0 and [Minimal "history plugins" for Camunda Cockpit](https://github.com/datakurre/camunda-cockpit-plugins) plugin

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile-history -t camunda-bpm-platform:7.18.0-jsonforms-history .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.18.0-jsonforms-history
```

Open <http://localhost:8080/camunda>

---

### Continuous Integration

The Camunda JSONForms Plugin project is built and tested via Github actions on Linux.

Current status: ![Build status](https://github.com/kchobantonov/camunda-jsonforms-plugin/actions/workflows/maven.yml/badge.svg?branch=master)
