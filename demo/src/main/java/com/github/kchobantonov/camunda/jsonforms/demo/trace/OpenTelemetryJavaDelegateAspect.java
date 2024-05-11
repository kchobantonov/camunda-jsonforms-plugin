package com.github.kchobantonov.camunda.jsonforms.demo.trace;

import java.lang.reflect.Field;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.camunda.bpm.engine.impl.cmd.MessageEventReceivedCmd;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobEntity;
import org.camunda.bpm.engine.impl.util.ClassNameUtil;
import org.springframework.util.ReflectionUtils;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

@Aspect
public class OpenTelemetryJavaDelegateAspect {
  private boolean enabled = OpenTelemetryUtil.isTracingEnabled();

  @Around("execution(* org.camunda.bpm.engine.delegate.JavaDelegate.execute(..)) && args(delegateExecution)")
  public Object spanAroundJavaDelegate(ProceedingJoinPoint joinPoint, DelegateExecution delegateExecution)
      throws Throwable {
    if (!enabled) {
      return joinPoint.proceed();
    }

    String currentActivityId = delegateExecution.getCurrentActivityId();
    String currentActivityName = delegateExecution.getCurrentActivityName();
    String processDefinitionId = delegateExecution.getProcessDefinitionId();
    String processInstanceId = delegateExecution.getProcessInstanceId();
    String businessKey = delegateExecution.getBusinessKey();

    String beanName = joinPoint.getTarget().getClass().getSimpleName();

    Span span = OpenTelemetryUtil.getTracer().spanBuilder(beanName + ".execute").startSpan();
    try (Scope scope = span.makeCurrent()) {

      span.setAttribute("componentType", "JavaDelegate");
      span.setAttribute("currentActivityId", currentActivityId);
      span.setAttribute("currentActivityName", currentActivityName);
      span.setAttribute("processDefinitionId", processDefinitionId);
      span.setAttribute("processInstanceId", processInstanceId);
      span.setAttribute("businessKey", businessKey);

      return joinPoint.proceed();
    } catch (Throwable t) {
      span.recordException(t);
      throw t;
    } finally {
      span.end();
    }

  }

  @Around(value = "execution(* org.camunda.bpm.engine.impl.interceptor.Command.execute(..)) && args(commandContext)")
  public Object spanAroundMessageEventReceived(ProceedingJoinPoint joinPoint, CommandContext commandContext)
      throws Throwable {
    if (!enabled) {
      return joinPoint.proceed();
    }

    Command<?> command = (Command<?>) joinPoint.getThis();

    Span span = OpenTelemetryUtil.getTracer()
        .spanBuilder(ClassNameUtil.getClassNameWithoutPackage(command.getClass()) + ".execute").startSpan();

    try (Scope scope = span.makeCurrent()) {

      handleMessageEventReceivedCmd(command, span);
      handleExecuteJobsCmd(command, span, commandContext);

      return joinPoint.proceed();
    } catch (Throwable t) {
      span.recordException(t);
      throw t;
    } finally {
      span.end();
    }
  }

  private void handleExecuteJobsCmd(Command<?> command, Span span, CommandContext commandContext) {
    if (command instanceof ExecuteJobsCmd) {
      String jobId = getField(command, "jobId", String.class);

      if (jobId != null) {
        final JobEntity job = commandContext.getDbEntityManager().selectById(JobEntity.class, jobId);
        if (job != null) {
          span.setAttribute("activityId", job.getActivityId());
          span.setAttribute("executionId", job.getExecutionId());
          span.setAttribute("processDefinitionKey", job.getProcessDefinitionKey());
          span.setAttribute("processInstanceId", job.getProcessInstanceId());
          ExecutionEntity execution = job.getExecution();
          if (execution != null) {
            span.setAttribute("businessKey", execution.getBusinessKey());
          }
        }
      }
    }
  }

  private void handleMessageEventReceivedCmd(Command<?> command, Span span) {
    if (command instanceof MessageEventReceivedCmd) {
      String messageName = getField(command, "messageName", String.class);
      String executionId = getField(command, "executionId", String.class);
      Map<String, Object> processVariables = getField(command, "processVariables", Map.class);
      Map<String, Object> processVariablesLocal = getField(command, "processVariablesLocal",
          Map.class);
      Map<String, Object> processVariablesToTriggeredScope = getField(command,
          "processVariablesToTriggeredScope", Map.class);

      span.setAttribute("messageName", messageName);
      span.setAttribute("executionId", executionId);
      span.setAttribute("processVariables", processVariables != null ? processVariables.toString() : null);
      span.setAttribute("processVariablesLocal",
          processVariablesLocal != null ? processVariablesLocal.toString() : null);
      span.setAttribute("processVariablesToTriggeredScope",
          processVariablesToTriggeredScope != null ? processVariablesToTriggeredScope.toString() : null);
    }
  }

  // Helper method to access a field in an object
  private <T> T getField(Object target, String fieldName, Class<T> fieldType) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    if (field != null) {
      ReflectionUtils.makeAccessible(field);
      return (T) ReflectionUtils.getField(field, target);
    } else {
      throw new IllegalArgumentException(
          "Field '" + fieldName + "' not found in class " + target.getClass().getName());
    }
  }

}
