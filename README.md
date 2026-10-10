# [Camunda](https://camunda.com/) [JSON Forms](https://jsonforms.io/) Plugin

Provides client-side and server-side integration for using Camunda Embedded Forms with JSON Forms using [JSON Forms Vuetify renderers](https://github.com/eclipsesource/jsonforms-vuetify-renderers).

## Form variables

The form's `*.schema.json` has `type: object`: its root properties are the process variables.
For example, `properties.firstName: { "type": "string" }` defines a String variable named
`firstName`; the form itself is not a `type: string` schema. Nested properties remain inside
their parent variable. The client derives the default type:

| JSON Schema type | Camunda variable type |
| --- | --- |
| `string` | `String` |
| `integer` | `Integer` |
| `number` | `Double` |
| `boolean` | `Boolean` |
| `object` or `array` | `Json` (Spin JSON) |
| `string` with `format: binary` | `File` |
| `null` | `Null` |

A nullable scalar such as `"type": ["string", "null"]` still maps to `String`.
Multiple non-null types map to `Json`. Absent, undefined and `readOnly` form properties
are skipped on submission; a supplied null is sent as null with its resolved variable type.

### Choose JSON or ArrayList for an array

Arrays use Spin JSON by default. To store a Java collection, add
`"x-camunda": { "type": "Object" }` to the root property:

```json
{
  "type": "object",
  "properties": {
    "jsonTags": {
      "type": "array",
      "items": { "type": "string" }
    },
    "listTags": {
      "type": "array",
      "items": { "type": "string" },
      "x-camunda": { "type": "Object" }
    }
  }
}
```

Given `["rush"]` for both fields, the client sends these REST variables automatically:

```json
{
  "variables": {
    "jsonTags": {
      "type": "Json",
      "value": "[\"rush\"]",
      "valueInfo": {}
    },
    "listTags": {
      "type": "Object",
      "value": "[\"rush\"]",
      "valueInfo": {
        "objectTypeName": "java.util.ArrayList<java.lang.String>",
        "serializationDataFormat": "application/json"
      }
    }
  }
}
```

Both values travel as JSON text. `jsonTags` becomes a Spin JSON node, while `listTags`
becomes an `ArrayList<String>` when the engine deserializes it. A process expression can
use `${listTags.contains('rush')}` or `${listTags.size()}`; Spin JSON uses APIs such as
`${jsonTags.elements()}`. `ArrayList` is an `objectTypeName`, not a REST variable `type`.
Use `"x-camunda": { "type": "Json" }` to select Spin JSON explicitly.

The engine must have the Spin process engine plugin and its Jackson JSON data format installed.
Changing a deployed variable from Spin JSON to a Java collection also changes the API its
process expressions consume, so coordinate the form change with the process version.

### Derived and explicit Java types

With `x-camunda.type: Object`, the client derives the following names:

| Property schema | Derived `objectTypeName` |
| --- | --- |
| Array of strings | `java.util.ArrayList<java.lang.String>` |
| Array of integers | `java.util.ArrayList<java.lang.Integer>` |
| Array of numbers | `java.util.ArrayList<java.lang.Double>` |
| Array of booleans | `java.util.ArrayList<java.lang.Boolean>` |
| Any of those arrays with `uniqueItems: true` | `java.util.LinkedHashSet<item type>` |
| Object without `properties`, with scalar `additionalProperties` | `java.util.LinkedHashMap<java.lang.String,item type>` |

Supply `valueInfo.objectTypeName` to override the derived name. For example, this keeps an
ArrayList even though the schema requires unique items:

```json
{
  "type": "array",
  "uniqueItems": true,
  "items": { "type": "string" },
  "x-camunda": {
    "type": "Object",
    "valueInfo": {
      "objectTypeName": "java.util.ArrayList<java.lang.String>"
    }
  }
}
```

Arrays of objects, tuples, unresolved item references, mixed item types and fixed-property
objects need an explicit name when stored as `Object`. For example,
`java.util.ArrayList<com.example.Order>` requires that class on the engine's classpath.
The form fails to load if an Object mapping has neither a derivable nor an explicit name.
Java array names use JVM descriptor syntax such as `[Ljava.lang.String;`, not `String[]`.
The browser cannot verify whether an explicitly named class exists or can be deserialized.

Object variables always use `application/json` serialization; another format is rejected.
`x-camunda.valueInfo` also supports metadata such as `"transient": true`, without requiring
a type override:

```json
{
  "type": "string",
  "x-camunda": { "valueInfo": { "transient": true } }
}
```

### Button variables

A UI schema `Button` can submit variables alongside the form data:

```json
{
  "type": "Button",
  "action": "camunda:complete",
  "text": "Approve",
  "variables": {
    "decision": { "type": "String", "value": "approved" },
    "listTags": { "value": ["rush"] }
  }
}
```

Using the schema above, `listTags` inherits its Object descriptor and is serialized as an
ArrayList. Explicit button `type` and `valueInfo` override the property's descriptor, and
button values take precedence over form values with the same name. Arrays and objects are
serialized automatically; existing serialized Json/Object strings and base64 File values
are preserved for compatibility. An undeclared button variable without a type is left for
the engine to infer.

This mapping applies to `camunda:submit`, `camunda:complete`, `camunda:resolve`, their
`-without-data` variants, `camunda:error` and `camunda:escalation`. The `-without-data`
actions send only button variables. If the schema declares `additionalProperties: false`,
all button variables must also be declared in its `properties` for validation to accept them
(including `decision` in this example).

### Reading and validating variables

The client requests `deserializeValues=false` and decodes both Spin JSON and JSON-serialized
Objects into ordinary form data. Non-JSON serialized Objects are omitted with a warning.
Zero, false and empty strings are preserved. `writeOnly` properties are not loaded.

Null is included in form data when the property's `type`, `enum`, `const`, `oneOf` or
`anyOf` declaration admits it; otherwise it is omitted. This is a lightweight check, not a
full schema evaluator for references or intersecting constraints. Backend handling of required
nullable properties remains a limitation: a submitted Java null is currently treated as an
absent property during validation.

File uploads use base64 data URLs. File variables are currently omitted from form data on read;
file metadata display and download controls are not implemented by this mapping.

The backend validates JSON-serialized Object values from their serialized text without loading
the named Java class. Java Maps and collections are normalized into JSON structures before
validation. Schema enforcement on task completion requires the opt-in configuration below.

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

The demo enables this property in `demo/src/main/resources/application.yaml`. The plugin
default remains disabled when the property is absent or false. It also applies to
`completeWithVariablesInReturn`; the task still needs one of the validator declarations above.

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

### Variable mapping tests

```bash
pnpm --dir plugin-ui/packages/camunda test
mvn -pl plugin test
```

The shared cases in `plugin-ui/packages/camunda/spec/variable-mapping.json` contain complete
object-root form schemas, form data and expected REST variables. UI tests check encoding,
reading, submission and schema errors; Maven copies the same cases into its test resources.
Camunda engine tests pass those payloads through `VariableValueDto`, submit deployed BPMN
user tasks, and check stored types, values and serialized readback.

Coverage includes primitives, Spin JSON, typed ArrayLists, ordered sets, maps, Java arrays,
optional nulls, file bytes and metadata, invalid item types and duplicate unique items.
Serialized Object formats other than JSON are rejected. BPMN expressions exercise both Java
collection and Spin APIs, and a service task verifies transient variables disappear at the
next wait state. The completion-property tests load the actual Spring configuration with
`validate-on-complete` unset, false and true, then verify invalid submissions, correction and
resubmission, and tasks without validators. These are in-memory Camunda engine tests, not
HTTP-server integration tests.

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
