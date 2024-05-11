package com.github.kchobantonov.camunda.jsonforms.demo.trace;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutorContext;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.engine.impl.util.ClassNameUtil;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class OpenTelemetryCommandInterceptor extends CommandInterceptor {

  @Override
  public <T> T execute(Command<T> command) {
    String activityId = null;
    String executionId = null;
    String processDefinitionKey = null;
    String processInstanceId = null;

    Span span = OpenTelemetryUtil.getTracer().spanBuilder("CommandInterceptor.execute").startSpan();
    try (Scope scope = span.makeCurrent()) {

      JobExecutorContext ctx = Context.getJobExecutorContext();
      if (ctx != null) {
        JobEntity job = ctx.getCurrentJob();
        if (job != null) {
          activityId = job.getActivityId();
          executionId = job.getExecutionId();
          processDefinitionKey = job.getProcessDefinitionKey();
          processInstanceId = job.getProcessInstanceId();
        }
      }

      span.setAttribute("componentType", "Command");
      span.setAttribute("command", ClassNameUtil.getClassNameWithoutPackage(command.getClass()));
      span.setAttribute("activityId", activityId);
      span.setAttribute("executionId", executionId);
      span.setAttribute("processDefinitionKey", processDefinitionKey);
      span.setAttribute("processInstanceId", processInstanceId);

      return next.execute(command);

    } catch (Throwable t) {
      span.recordException(t);
      throw t;
    } finally {
      span.end();
    }

  }

}
