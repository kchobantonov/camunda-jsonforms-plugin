package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Map;

import org.camunda.bpm.engine.impl.TaskServiceImpl;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;

/**
 * A {@link org.camunda.bpm.engine.TaskService} that holds {@code complete} to the same form
 * schema {@code submitTaskForm} is held to.
 *
 * <h2>The gap this closes</h2>
 *
 * Validation lives in {@link JsonFormsFormHandler#submitFormVariables}, and the form handler is
 * only invoked by {@code FormService.submitTaskForm}. {@code TaskService.complete} runs
 * {@code CompleteTaskCmd}, which never touches it. That is Camunda's design rather than an
 * oversight - {@code complete} is meant to be the formless path - but the consequence is that
 * every JSON schema in an application is enforced on one route and not the other, including the
 * engine's own REST API, whose {@code POST /task/{id}/complete} maps straight onto it.
 *
 * So a caller with access to that endpoint could submit whatever a form declares
 * {@code readOnly}, whatever it pins with {@code const}, and whatever the schema refuses under
 * {@code additionalProperties: false} - and the only defence was whatever the process's own
 * delegates happened to re-check.
 *
 * <h2>How</h2>
 *
 * The submitted variables are the argument here, which is what makes the full schema applicable
 * rather than only part of it. A task listener on {@code complete} would see the merged variable
 * scope instead and could not tell a submitted value from a process variable that was already
 * there, so it could never enforce {@code additionalProperties: false} - the clause that
 * defends a process's own identifiers from being rewritten by a completion.
 *
 * Validation is run and then the completion is delegated unchanged, rather than being routed
 * through {@code submitTaskForm}. The two are not interchangeable: submitting a form records
 * form-field history that completing does not, so re-routing would quietly change the historic
 * data every existing consumer sees.
 *
 * <h2>Scope</h2>
 *
 * Opt-in, by the same mechanism as before: {@link JsonFormsFormHandler#validate} does nothing
 * unless the user task declares a {@code validator} constraint. A process that never asked for
 * JsonForms validation completes exactly as it did.
 *
 * @see JsonFormsTaskServicePlugin
 */
public class JsonFormsTaskService extends TaskServiceImpl {

  @Override
  public void complete(String taskId) {
    complete(taskId, null);
  }

  @Override
  public void complete(String taskId, Map<String, Object> variables) {
    validateSubmission(taskId, variables);
    super.complete(taskId, variables);
  }

  @Override
  public VariableMap completeWithVariablesInReturn(String taskId, Map<String, Object> variables,
      boolean deserializeValues) {
    validateSubmission(taskId, variables);
    return super.completeWithVariablesInReturn(taskId, variables, deserializeValues);
  }

  /**
   * Refuses the completion if the task carries a form the submission does not satisfy.
   *
   * All of it inside one command, because none of the work can be done outside one. Resolving
   * the schema reads the deployment through {@code Context.getCommandContext()}, and the
   * validator resolves its expression against the task's execution, which a {@code TaskEntity}
   * only fetches through the same context. Looking the task up in a command and then validating
   * after it returns compiles perfectly and fails with a {@code NullPointerException} the
   * moment a task actually carries a validator.
   *
   * Its own command rather than part of the completion's: nothing is written here, so a refusal
   * leaves no work to unwind, and reimplementing {@code CompleteTaskCmd} to share one command
   * would be a far larger thing to keep correct across engine versions than a read.
   *
   * A task that cannot be found is left alone - {@code super.complete} is about to raise the
   * engine's own not-found, which says it better than anything this could invent.
   */
  protected void validateSubmission(String taskId, Map<String, Object> variables) {
    VariableMap submitted = variables == null ? Variables.createVariables()
        : Variables.fromMap(variables);

    commandExecutor.execute(new Command<Void>() {
      @Override
      public Void execute(CommandContext commandContext) {
        TaskEntity task = commandContext.getTaskManager().findTaskById(taskId);
        if (task == null) {
          return null;
        }
        TaskDefinition definition = task.getTaskDefinition();
        TaskFormHandler handler = definition == null ? null : definition.getTaskFormHandler();

        if (handler instanceof JsonFormsFormHandler) {
          ((JsonFormsFormHandler) handler).validate(submitted, task);
        }
        return null;
      }
    });
  }
}
