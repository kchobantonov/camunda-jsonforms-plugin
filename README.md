# [Camunda](https://camunda.com/) [JSONForms](https://jsonforms.io/) Plugin

Provides client-side and server-side integration for using Camunda Embedded Forms with JSONForms.

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
http://localhost:8080/camunda

Note: docker image will be based on camumda-bpm-platform and will not include the demo project.
