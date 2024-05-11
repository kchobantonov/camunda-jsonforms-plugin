package com.github.kchobantonov.camunda.jsonforms.demo.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public final class OpenTelemetryUtil {
  private static boolean openTelemetryEnabled = false;

  static {
    // To check if the opentelemetry is enabled, we check if the result from the
    // very first call to GlobalOpenTelemetry.get() returns OpenTelemetry.noop()
    openTelemetryEnabled = GlobalOpenTelemetry.get() != OpenTelemetry.noop();
  }

  private static final Tracer tracer = GlobalOpenTelemetry.getTracer("com.github.kchobantonov.camunda.jsonforms.demo",
      "0.0.1");

  private OpenTelemetryUtil() {
  }

  public static Tracer getTracer() {
    return tracer;
  }

  public static boolean isTracingEnabled() {
    return isOpenTelemetryEnabled() && Boolean.parseBoolean(getDemoInstrumentationEnabledProperty());
  }

  public static boolean isOpenTelemetryEnabled() {
    return openTelemetryEnabled;
  }

  private static String getDemoInstrumentationEnabledProperty() {
    try {
      String propVal = System.getProperty("otel.instrumentation.demo.enabled");
      if (propVal == null) {
        // Fall back to searching the system environment.
        propVal = System.getenv("OTEL_INSTRUMENTATION_DEMO_ENABLED");
      }
      if (propVal == null) {
        // default is true
        propVal = "true";
      }
      return propVal;
    } catch (Throwable ex) {
      return null;
    }
  }

}
