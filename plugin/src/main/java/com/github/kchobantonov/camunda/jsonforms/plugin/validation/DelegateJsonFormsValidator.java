package com.github.kchobantonov.camunda.jsonforms.plugin.validation;

import java.util.Map;
import java.util.concurrent.Callable;

import org.camunda.bpm.application.InvocationContext;
import org.camunda.bpm.application.ProcessApplicationReference;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.util.ReflectUtil;


public class DelegateJsonFormsValidator implements JsonFormsValidator {

  protected String clazz;
  protected Expression delegateExpression;

  public DelegateJsonFormsValidator(Expression expression) {
    delegateExpression = expression;
  }

  public DelegateJsonFormsValidator(String clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean validate(final Map<String, Object> submittedValues,
      final VariableScope variableScope) {

    final DelegateExecution execution = getExecution(variableScope);

    if (shouldPerformPaContextSwitch(execution)) {
      ProcessApplicationReference processApplicationReference = ProcessApplicationContextUtil
          .getTargetProcessApplication((ExecutionEntity) execution);

      return Context.executeWithinProcessApplication(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          return doValidate(submittedValues, variableScope);
        }
      }, processApplicationReference, new InvocationContext(execution));

    } else {
      return doValidate(submittedValues, variableScope);

    }

  }

  public DelegateExecution getExecution(VariableScope variableScope) {
    if (variableScope instanceof DelegateExecution) {
      return (DelegateExecution) variableScope;
    } else if (variableScope instanceof TaskEntity) {
      return ((TaskEntity) variableScope).getExecution();
    } else {
      return null;
    }
  }

  protected boolean shouldPerformPaContextSwitch(DelegateExecution execution) {
    if (execution == null) {
      return false;
    } else {
      ProcessApplicationReference targetPa = ProcessApplicationContextUtil
          .getTargetProcessApplication((ExecutionEntity) execution);
      return targetPa != null && !targetPa.equals(Context.getCurrentProcessApplication());
    }
  }

  protected boolean doValidate(final Map<String, Object> submittedValues,
      final VariableScope variableScope) {
    JsonFormsValidator validator;

    if (clazz != null) {
      // resolve validator using Fully Qualified Classname
      Object validatorObject = ReflectUtil.instantiate(clazz);
      if (validatorObject instanceof JsonFormsValidator) {
        validator = (JsonFormsValidator) validatorObject;

      } else {
        throw new ProcessEngineException(
            "Validator class '" + clazz + "' is not an instance of " + JsonFormsValidator.class.getName());

      }
    } else {
      // resolve validator using expression
      Object validatorObject = delegateExpression.getValue(getExecution(variableScope));
      if (validatorObject instanceof JsonFormsValidator) {
        validator = (JsonFormsValidator) validatorObject;

      } else {
        throw new ProcessEngineException("Validator expression '" + delegateExpression
            + "' does not resolve to instance of " + JsonFormsValidator.class.getName());

      }
    }

    JsonFormsValidatorInvocation invocation = new JsonFormsValidatorInvocation(validator, submittedValues,
        variableScope);
    try {
      Context
          .getProcessEngineConfiguration()
          .getDelegateInterceptor()
          .handleInvocation(invocation);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ProcessEngineException(e);
    }

    return invocation.getInvocationResult();
  }

}