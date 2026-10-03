package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidator;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator;
import com.github.kchobantonov.camunda.jsonforms.plugin.validation.JsonFormsValidator;

/**
 * Whether a task completed <em>without</em> a form is still held to the form's schema.
 *
 * Validation lives in the form handler, and only {@code FormService.submitTaskForm} invokes one.
 * {@code TaskService.complete} runs {@code CompleteTaskCmd}, which does not - so every schema in
 * an application was enforced on one route and not the other, and the engine's own REST API
 * {@code POST /task/{id}/complete} maps straight onto the route with no validation.
 *
 * Driven through a real in-memory engine, because the whole question is which engine command
 * path reaches the validator. Stand-ins would only prove that the code calls what it was written
 * to call.
 */
class JsonFormsTaskServiceTest {

  private static final String PROCESS = "CompletionTest";
  private static final String BPMN = "completion.bpmn";

  /** The same form, with the validator declared as a {@code camunda:property} instead. */
  private static final String PROPERTY_PROCESS = "CompletionPropertyTest";
  private static final String PROPERTY_BPMN = "completion-property.bpmn";

  /** The same form again, declaring both at once, each naming a different validator. */
  private static final String BOTH_PROCESS = "CompletionBothTest";
  private static final String BOTH_BPMN = "completion-both.bpmn";

  private ProcessEngine engine;

  /** The bean {@code ${jsonFormsValidator}} resolves to - counts its runs, validates for real. */
  private CountingValidator validator;

  /**
   * The bean {@code ${permissiveValidator}} resolves to: counts, and passes anything.
   *
   * Only {@code completion-both.bpmn} names it, on its form field, so that picking it instead of
   * the property's validator is not a subtle difference in bookkeeping but the schema going
   * unenforced - which is what the precedence test asserts against.
   */
  private PermissiveValidator permissiveValidator;

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  // ---- the gap, and that it is closed -------------------------------------------------

  /**
   * Load-bearing. {@code confirmed} is pinned to {@code const: true} by the schema, and the
   * submission says false - the shape of "I did not do the check, grant it anyway". Through
   * {@code submitTaskForm} this was always refused; through {@code complete} it was accepted.
   */
  @Test
  void aCompletionThatViolatesTheSchemaIsRefused() {
    String taskId = startAndGetTask(withValidation());

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, accept(false, "checked")));

    assertNotNull(task(), "the task must still be open after a refused completion");
  }

  /** The clause a completion-time listener could never enforce - it cannot see what was submitted. */
  @Test
  void aCompletionCarryingAnUndeclaredVariableIsRefused() {
    String taskId = startAndGetTask(withValidation());

    Map<String, Object> submitted = accept(true, "checked");
    submitted.put("smuggled", "value");

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, submitted));
  }

  /** {@code readOnly} means the form shows it; it does not mean a completion may rewrite it. */
  @Test
  void aCompletionRewritingAReadOnlyValueIsRefused() {
    String taskId = startAndGetTask(withValidation());

    Map<String, Object> submitted = accept(true, "checked");
    submitted.put("subjectId", "somebody-else");

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, submitted));
  }

  /** A conditional requirement, missed on the same route. */
  @Test
  void aCompletionMissingAConditionallyRequiredFieldIsRefused() {
    String taskId = startAndGetTask(withValidation());

    Map<String, Object> submitted = new HashMap<>();
    submitted.put("action", "accept");
    submitted.put("confirmed", Boolean.TRUE);
    // no notes, which the schema requires when the action is accept

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, submitted));
  }

  /** And a valid one still completes - the point is a gate, not a wall. */
  @Test
  void aCompletionThatSatisfiesTheSchemaGoesThrough() {
    String taskId = startAndGetTask(withValidation());

    engine.getTaskService().complete(taskId, accept(true, "identity checked by phone"));

    assertNull(task(), "the task should be gone once completed");
    assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().count());
  }

  /** The branch with no extra requirements still works - the conditional is respected. */
  @Test
  void aCompletionOnTheOtherBranchIsNotHeldToTheAcceptRules() {
    String taskId = startAndGetTask(withValidation());

    Map<String, Object> submitted = new HashMap<>();
    submitted.put("action", "decline");

    engine.getTaskService().complete(taskId, submitted);

    assertNull(task());
  }

  // ---- that the change is what closed it ----------------------------------------------

  /**
   * The counterpart, and what makes the rest of this file mean something: with the plugin left
   * out, the identical completion is accepted. This is the behaviour every application on this
   * library has today.
   */
  @Test
  void withoutThePluginTheSameCompletionIsAccepted() {
    String taskId = startAndGetTask(withoutTaskServicePlugin());

    engine.getTaskService().complete(taskId, accept(false, "checked"));

    assertNull(task(), "unguarded, complete() never reaches the schema");
  }

  /** Belt and braces: the route that always validated still does, with the plugin installed. */
  @Test
  void submitTaskFormStillValidatesAsItAlwaysDid() {
    String taskId = startAndGetTask(withValidation());

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getFormService().submitTaskForm(taskId, accept(false, "checked")));
  }

  // ---- once, and only once ---------------------------------------------------------------

  /**
   * What lets {@link JsonFormsFormHandler#submitFormVariables} skip a validator it read off a
   * form field: the engine runs that same constraint itself, so running it here as well would
   * put every submitted form through the schema twice.
   *
   * The submission has to be a <em>valid</em> one for this to mean anything. An invalid one is
   * refused by whichever run comes first and would count 1 either way; only a submission that
   * gets all the way through can show the second run that is not there. Remove the skip in
   * {@code submitFormVariables} and this is 2.
   */
  @Test
  void submitTaskFormRunsAFormFieldValidatorOnceNotTwice() {
    String taskId = startAndGetTask(withValidation());

    engine.getFormService().submitTaskForm(taskId, accept(true, "identity checked by phone"));

    assertEquals(1, validator.runs(),
        "the engine runs the form field constraint; the form handler must not run it again");
    assertNull(task());
  }

  /**
   * And the other side of that skip: on {@code complete} the engine runs no form handler and no
   * form field validation, so this library's own run is the only one there is. It has to happen,
   * and it has to happen once.
   */
  @Test
  void completeRunsAFormFieldValidatorOnceNotTwice() {
    String taskId = startAndGetTask(withValidation());

    engine.getTaskService().complete(taskId, accept(true, "identity checked by phone"));

    assertEquals(1, validator.runs(), "complete() reaches the schema exactly once");
    assertNull(task());
  }

  /**
   * The skip is narrow, and this is what holds it to that.
   *
   * A validator named by a {@code camunda:property} is not a form field constraint, so the
   * engine knows nothing about it and never runs it. If {@code submitFormVariables} skipped
   * that one too, this form would be submitted with nothing checking it at all - the schema
   * silently switched off for every process that declares its validator this way.
   */
  @Test
  void submitTaskFormStillRunsAValidatorDeclaredAsAProperty() {
    String taskId = startAndGetTask(withValidation(), PROPERTY_BPMN, PROPERTY_PROCESS);

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getFormService().submitTaskForm(taskId, accept(false, "checked")));

    assertEquals(1, validator.runs(), "nobody but the form handler runs a property validator");
    assertNotNull(task(), "the task must still be open after a refused submission");
  }

  /** The same declaration, counted on a submission that passes: run, and run once. */
  @Test
  void submitTaskFormRunsAPropertyValidatorOnceNotTwice() {
    String taskId = startAndGetTask(withValidation(), PROPERTY_BPMN, PROPERTY_PROCESS);

    engine.getFormService().submitTaskForm(taskId, accept(true, "identity checked by phone"));

    assertEquals(1, validator.runs());
    assertNull(task());
  }

  /** And {@code complete} is guarded whichever way the task declares its validator. */
  @Test
  void completeIsGuardedByAValidatorDeclaredAsAProperty() {
    String taskId = startAndGetTask(withValidation(), PROPERTY_BPMN, PROPERTY_PROCESS);

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, accept(false, "checked")));

    assertEquals(1, validator.runs());
    assertNotNull(task());
  }

  // ---- which declaration wins --------------------------------------------------------------

  /**
   * A task declaring both: the {@code camunda:property} is the one in force.
   *
   * That is the preferred way to name a validator, and the form field constraint is the older
   * one, so a task that carries both has been migrated and means the property. The two name
   * different validators here and the form field's refuses nothing, which turns the question
   * into one the test can answer without reading the handler: a submission that violates the
   * schema is refused, so the property's validator is what ran.
   */
  @Test
  void thePropertyValidatorWinsOverTheFormFieldOne() {
    String taskId = startAndGetTask(withValidation(), BOTH_BPMN, BOTH_PROCESS);

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, accept(false, "checked")));

    assertEquals(1, validator.runs(), "the property's validator is the one that runs");
    assertEquals(0, permissiveValidator.runs(), "the form field's is not consulted");
    assertNotNull(task());
  }

  /** The same on the other route, where the form handler is invoked rather than bypassed. */
  @Test
  void thePropertyValidatorWinsOnSubmitTaskFormToo() {
    String taskId = startAndGetTask(withValidation(), BOTH_BPMN, BOTH_PROCESS);

    assertThrows(JsonFormsValidatorException.class,
        () -> engine.getFormService().submitTaskForm(taskId, accept(false, "checked")));

    assertEquals(1, validator.runs(), "the property's validator is the one that runs");
    assertNotNull(task());
  }

  /**
   * And the skip does not follow the losing declaration.
   *
   * The form field constraint still exists, so the engine still runs it on {@code
   * submitTaskForm} - that is the engine's doing and not something this library can call off.
   * What matters is that the form handler does not treat that as a reason to skip its own run:
   * the engine is running the <em>other</em> validator, so skipping would leave the property's
   * one unrun on the very path it was written for. Two runs here is the cost of declaring two
   * validators, and it is the BPMN saying so.
   */
  @Test
  void aValidSubmissionRunsBothDeclaredValidatorsOnceEach() {
    String taskId = startAndGetTask(withValidation(), BOTH_BPMN, BOTH_PROCESS);

    engine.getFormService().submitTaskForm(taskId, accept(true, "identity checked by phone"));

    assertEquals(1, validator.runs(), "the property's validator, run by the form handler");
    assertEquals(1, permissiveValidator.runs(), "the form field's, run by the engine");
    assertNull(task());
  }

  // ---- opt-in ---------------------------------------------------------------------------

  /**
   * A task that never asked for JsonForms validation is unaffected. This matters more than it
   * looks: the plugin is installed engine-wide, and every other process on the engine keeps
   * completing exactly as it did.
   */
  @Test
  void aTaskWithNoValidatorIsCompletedUnchanged() {
    String taskId = startAndGetTask(withValidationRemoved());

    Map<String, Object> submitted = new HashMap<>();
    submitted.put("anything", "at all");

    engine.getTaskService().complete(taskId, submitted);

    assertNull(task());
  }

  /** The error has to name the field, or the caller is told only that something was wrong. */
  @Test
  void theRefusalNamesTheFieldThatFailed() {
    String taskId = startAndGetTask(withValidation());

    JsonFormsValidatorException thrown = assertThrows(JsonFormsValidatorException.class,
        () -> engine.getTaskService().complete(taskId, accept(false, "checked")));

    List<String> paths = thrown.toJsonFormsErrors().stream()
        .map(JsonFormsErrorObject::getInstancePath)
        .toList();
    assertTrue(paths.contains("/confirmed"), "expected an error on /confirmed, got " + paths);
  }

  /**
   * Pins a deliberate fail-open, so that changing it is a decision rather than an accident.
   *
   * With no schema resource in the deployment there is nothing to validate against, and
   * {@code DefaultJsonFormsValidator} passes the submission. Refusing instead would take down
   * every form whose schema has not been deployed yet, which is not that class's call - but it
   * does mean a renamed or omitted {@code .schema.json} disables {@code readOnly},
   * {@code const} and {@code additionalProperties} for that form, everywhere, at once. It now
   * says so in the log; this is here so the silence cannot come back unnoticed.
   */
  @Test
  void aFormWhoseSchemaWasNeverDeployedIsNotValidated() {
    engine = withValidation().buildProcessEngine();
    engine.getRepositoryService().createDeployment()
        .addClasspathResource("completion.bpmn")   // and deliberately not the schema
        .deploy();

    ProcessInstance instance = engine.getRuntimeService().startProcessInstanceByKey(PROCESS);
    String taskId = engine.getTaskService().createTaskQuery()
        .processInstanceId(instance.getId()).singleResult().getId();

    engine.getTaskService().complete(taskId, accept(false, "checked"));

    assertNull(task(), "with no schema there is nothing to refuse against");
  }

  // ---- fixture --------------------------------------------------------------------------

  private Map<String, Object> accept(boolean confirmed, String notes) {
    Map<String, Object> submitted = new HashMap<>();
    submitted.put("action", "accept");
    submitted.put("confirmed", confirmed);
    submitted.put("notes", notes);
    return submitted;
  }

  private Task task() {
    return engine.getTaskService().createTaskQuery().singleResult();
  }

  private String startAndGetTask(ProcessEngineConfigurationImpl configuration) {
    return startAndGetTask(configuration, BPMN, PROCESS);
  }

  private String startAndGetTask(ProcessEngineConfigurationImpl configuration,
      String bpmn, String processKey) {
    engine = configuration.buildProcessEngine();

    DeploymentBuilder deployment = engine.getRepositoryService().createDeployment()
        .addClasspathResource(bpmn)
        .addClasspathResource("forms/Decision.schema.json")
        .addClasspathResource("forms/Decision.json");
    deployment.deploy();

    ProcessInstance instance = engine.getRuntimeService().startProcessInstanceByKey(processKey);
    Task task = engine.getTaskService().createTaskQuery()
        .processInstanceId(instance.getId()).singleResult();
    assertNotNull(task, "the process should be waiting on the user task");
    return task.getId();
  }

  /** The engine as an application using this library would configure it, plugin included. */
  private ProcessEngineConfigurationImpl withValidation() {
    return engineConfiguration(true, true);
  }

  /** The same engine before this change: form rendering, but complete() unguarded. */
  private ProcessEngineConfigurationImpl withoutTaskServicePlugin() {
    return engineConfiguration(true, false);
  }

  /** The parse listener is what installs the validator; without it the task declares none. */
  private ProcessEngineConfigurationImpl withValidationRemoved() {
    return engineConfiguration(false, true);
  }

  private ProcessEngineConfigurationImpl engineConfiguration(boolean parseListener,
      boolean taskServicePlugin) {
    StandaloneInMemProcessEngineConfiguration configuration =
        new StandaloneInMemProcessEngineConfiguration();
    configuration.setJdbcUrl("jdbc:h2:mem:jsonforms-" + System.nanoTime()
        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    configuration.setProcessEngineName("jsonforms-" + System.nanoTime());
    configuration.setJobExecutorActivate(false);

    // ${jsonFormsValidator} in the BPMN resolves against the engine's bean map
    validator = new CountingValidator();
    permissiveValidator = new PermissiveValidator();
    Map<Object, Object> beans = new HashMap<>();
    beans.put("jsonFormsValidator", validator);
    beans.put("permissiveValidator", permissiveValidator);
    configuration.setBeans(beans);

    List<org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin> plugins = new java.util.ArrayList<>();
    if (parseListener) {
      plugins.add(new JsonFormsParseListenerProcessEnginePlugin());
    }
    if (taskServicePlugin) {
      plugins.add(new JsonFormsTaskServicePlugin());
    }
    configuration.setProcessEnginePlugins(plugins);

    return configuration;
  }

  /**
   * The real validator, counting how many times a submission reaches it.
   *
   * It wraps {@link DefaultJsonFormsValidator} rather than standing in for it, so every
   * assertion in this file still runs the actual schema; the count is the only thing added.
   * Both of the ways in funnel through {@code validate(Map, VariableScope)} - the engine's
   * form field validation arrives through {@link FormFieldValidator}, this library's through
   * {@link JsonFormsValidator} - which is exactly what makes a double run visible.
   */
  static class CountingValidator implements JsonFormsValidator, FormFieldValidator {

    private final DefaultJsonFormsValidator delegate = new DefaultJsonFormsValidator(null);
    private final AtomicInteger runs = new AtomicInteger();

    @Override
    public boolean validate(Map<String, Object> submittedValues, VariableScope variableScope) {
      runs.incrementAndGet();
      return delegate.validate(submittedValues, variableScope);
    }

    @Override
    public boolean validate(Object submittedValue, FormFieldValidatorContext context) {
      return validate(context.getSubmittedValues(), context.getVariableScope());
    }

    int runs() {
      return runs.get();
    }
  }

  /**
   * A validator that refuses nothing, and counts.
   *
   * It stands for "the wrong one was picked": if a submission this would have waved through is
   * refused, the other declaration is the one in force.
   */
  static class PermissiveValidator implements JsonFormsValidator, FormFieldValidator {

    private final AtomicInteger runs = new AtomicInteger();

    @Override
    public boolean validate(Map<String, Object> submittedValues, VariableScope variableScope) {
      runs.incrementAndGet();
      return true;
    }

    @Override
    public boolean validate(Object submittedValue, FormFieldValidatorContext context) {
      return validate(context.getSubmittedValues(), context.getVariableScope());
    }

    int runs() {
      return runs.get();
    }
  }
}
