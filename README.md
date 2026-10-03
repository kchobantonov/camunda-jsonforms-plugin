# [Camunda](https://camunda.com/) [JSON Forms](https://jsonforms.io/) Plugin

Provides client-side and server-side integration for using Camunda Embedded Forms with JSON Forms using [JSON Forms Vuetify renderers](https://github.com/eclipsesource/jsonforms-vuetify-renderers).

## Server-side schema validation

A user task declares its validator either as an extension property on the task:

```xml
<camunda:properties>
  <camunda:property name="jsonFormsValidator" value="${jsonFormsValidator}" />
</camunda:properties>
```

or, as most BPMNs in practice do, as a constraint on one of its form fields:

```xml
<camunda:formData>
  <camunda:formField id="data" type="string">
    <camunda:validation>
      <camunda:constraint name="validator" config="${jsonFormsValidator}" />
    </camunda:validation>
  </camunda:formField>
</camunda:formData>
```

Either way the submission is checked against the form's `*.schema.json`. The two
declarations are equivalent, and a form needs only one: the validator is handed the whole
submission, so declaring it on a second field would ask for the identical check twice.

### Completing a task without a form

`FormService.submitTaskForm` runs the form handler, and therefore the schema.
`TaskService.complete` does not - that is Camunda's design, `complete` being the formless
path - so a schema is enforced on one route and not the other, including the engine's own
`POST /task/{id}/complete`.

`JsonFormsTaskServicePlugin` closes that. It is **opt-in**, because it changes the behaviour
of an engine API an application may already be calling:

```properties
camunda.jsonforms.validate-on-complete=true
```

With it installed, a completion is held to the same schema a submission is: whatever the form
declares `readOnly`, whatever it pins with `const`, and whatever it refuses under
`additionalProperties: false`. A task whose form declares no validator completes exactly as
before.

Validation runs against the **submitted variables**, which is what makes the whole schema
applicable. A task listener on `complete` sees the merged variable scope instead and cannot
tell a submitted value from a process variable that was already there, so it could never
enforce `additionalProperties: false` - the clause that defends a process's own identifiers
from being rewritten by a completion.

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

### Docker

**Note**: The docker image is based on camumda-bpm-platform and will not include the demo project, instead the camunda-invoice application that is shipped with camunda-bpm-platform will be modified using the files under [camunda-invoice](./docker-camunda-bpm-platform/camunda-invoice). You can check the JSON Forms schema, uischema, i18n JSON files that are used for camunda-invoice demo under that folder.

Also if you get in the camunda cockpit the following error "The context path is either empty or not defined." while opening the task form then shutdown the server and start it again. It looks like there is an issue with camunda when the BPMN is deployed for the first time.

---

* With Camunda Platform 7.24.0

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile -t camunda-bpm-platform:7.24.0-jsonforms .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.24.0-jsonforms
```

Open <http://localhost:8080/camunda>

---

* With Camunda Platform 7.24.0 and [Minimal "history plugins" for Camunda Cockpit](https://github.com/kchobantonov/camunda-cockpit-plugins) plugin clone of [Minimal "history plugins" for Camunda Cockpit](https://github.com/datakurre/camunda-cockpit-plugins) plugin

```bash
git clone https://github.com/kchobantonov/camunda-jsonforms-plugin.git
docker build -f Dockerfile-history -t camunda-bpm-platform:7.24.0-jsonforms-history .
docker run --rm -p 8080:8080 camunda-bpm-platform:7.24.0-jsonforms-history
```

Open <http://localhost:8080/camunda>

---

### Continuous Integration

The Camunda JSONForms Plugin project is built and tested via Github actions on Linux.

Current status: ![Build status](https://github.com/kchobantonov/camunda-jsonforms-plugin/actions/workflows/maven.yml/badge.svg?branch=master)
