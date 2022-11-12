ARG ARCH=

FROM maven:3.8.6-openjdk-8 as compiler
WORKDIR /usr/local/src/
COPY . .
RUN mvn clean install

FROM ${ARCH}alpine:latest as builder

ARG VERSION=7.16.0
ARG DISTRO=tomcat
ARG SNAPSHOT=false

ARG EE=false
ARG USER
ARG PASSWORD

ARG MAVEN_PROXY_HOST
ARG MAVEN_PROXY_PORT
ARG MAVEN_PROXY_USER
ARG MAVEN_PROXY_PASSWORD

ARG JMX_PROMETHEUS_VERSION=0.12.0

RUN apk add --no-cache \
        bash \
        ca-certificates \
        maven \
        tar \
        wget \
        xmlstarlet

COPY --from=compiler /usr/local/src/docker-camunda-bpm-platform/settings.xml /tmp/
COPY --from=compiler /usr/local/src/docker-camunda-bpm-platform/download.sh /tmp/
COPY --from=compiler /usr/local/src/docker-camunda-bpm-platform/camunda-run.sh /tmp/
COPY --from=compiler /usr/local/src/docker-camunda-bpm-platform/camunda-tomcat.sh /tmp/
COPY --from=compiler /usr/local/src/docker-camunda-bpm-platform/camunda-wildfly.sh /tmp/

RUN chmod +x /tmp/*.sh
RUN /tmp/download.sh

COPY --chown=camunda:camunda --from=compiler /usr/local/src/plugin/target/*.jar /camunda/lib/
COPY --chown=camunda:camunda --from=compiler /usr/local/src/plugin/target/classes/META-INF/resources/webjars/camunda/ /camunda/webapps/camunda/
COPY --chown=camunda:camunda --from=compiler /usr/local/src/plugin/target/classes/META-INF/resources/webjars/forms/* /camunda/webapps/camunda-invoice/webjars/forms/
COPY --chown=camunda:camunda --from=compiler /usr/local/src/docker-camunda-bpm-platform/camunda-invoice/forms/ /camunda/webapps/camunda-invoice/forms/
COPY --chown=camunda:camunda --from=compiler /usr/local/src/docker-camunda-bpm-platform/camunda-invoice/classes/ /camunda/webapps/camunda-invoice/WEB-INF/classes

##### FINAL IMAGE #####

FROM ${ARCH}alpine:latest

ARG VERSION=7.16.0

ENV CAMUNDA_VERSION=${VERSION}
ENV DB_DRIVER=
ENV DB_URL=
ENV DB_USERNAME=
ENV DB_PASSWORD=
ENV DB_CONN_MAXACTIVE=20
ENV DB_CONN_MINIDLE=5
ENV DB_CONN_MAXIDLE=20
ENV DB_VALIDATE_ON_BORROW=false
ENV DB_VALIDATION_QUERY="SELECT 1"
ENV SKIP_DB_CONFIG=
ENV WAIT_FOR=
ENV WAIT_FOR_TIMEOUT=30
ENV TZ=UTC
ENV DEBUG=false
ENV JAVA_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m"
ENV JMX_PROMETHEUS=false
ENV JMX_PROMETHEUS_CONF=/camunda/javaagent/prometheus-jmx.yml
ENV JMX_PROMETHEUS_PORT=9404

EXPOSE 8080 8000 9404

# Downgrading wait-for-it is necessary until this PR is merged
# https://github.com/vishnubob/wait-for-it/pull/68
RUN apk add --no-cache \
        bash \
        ca-certificates \
        curl \
        openjdk11-jre-headless \
        tzdata \
        tini \
        xmlstarlet \
    && curl -o /usr/local/bin/wait-for-it.sh \
      "https://raw.githubusercontent.com/vishnubob/wait-for-it/a454892f3c2ebbc22bd15e446415b8fcb7c1cfa4/wait-for-it.sh" \
    && chmod +x /usr/local/bin/wait-for-it.sh

RUN addgroup -g 1000 -S camunda && \
    adduser -u 1000 -S camunda -G camunda -h /camunda -s /bin/bash -D camunda
WORKDIR /camunda
USER camunda

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["./camunda.sh"]

COPY --chown=camunda:camunda --from=builder /camunda .
