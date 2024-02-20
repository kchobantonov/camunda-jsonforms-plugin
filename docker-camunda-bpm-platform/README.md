# Camunda Platform Docker Images with JsonForms

## Supported Tags/Releases

- tag: tomcat
- version: 7.20.0

## Build
Make sure that you have ran ```mvn clean install``` on the top level project folder before that.

The image can be used to build a Docker image for a given Camunda Platform
version and distribution.

### Build a released version

To build a community image specify the `ARCH` build
argument. Possible values for `ARCH` are `amd64/`, `arm64v8/`.
This need to be run on the project top level folder.

```
docker build -t camunda-bpm-platform-jsonforms:7.20.0 \
  --build-arg ARCH=amd64/ \
  .
```
## Running the image

Read [README.md](https://github.com/camunda/docker-camunda-bpm-platform/blob/7.16/README.md) for different ways how to run that. 

Note that if you are adding additional web applications those needs to be configured properly before you can use them with JsonForms.

For example you can run the newly created image using the following command

```
docker run -d --name camunda-jsonforms -p 8080:8080 camunda-bpm-platform-jsonforms:7.20.0
```