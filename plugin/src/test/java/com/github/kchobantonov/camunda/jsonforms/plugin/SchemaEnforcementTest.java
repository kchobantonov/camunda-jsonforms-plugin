package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.spin.Spin;
import org.camunda.spin.json.SpinJsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator;

/**
 * What a schema means for a submission - stated without naming the library that decides it.
 *
 * Every assertion here goes in through {@code complete} or {@code submitTaskForm} and comes back
 * as a {@link JsonFormsValidatorException} or a completed task, so nothing in this file knows
 * whether everit, networknt or anything else is underneath. That is the point of it: these are
 * the answers a form gets today, and they are what a different validator would have to keep
 * giving for the swap to be a swap rather than a change in behaviour nobody asked for.
 *
 * {@code DefaultJsonFormsValidatorTest} is the opposite kind of test - it reaches into everit's
 * violation tree on purpose, because flattening that tree is this library's own work. Expect to
 * rewrite that one against any replacement; expect this one to pass unchanged.
 *
 * The form is {@code forms/Validation.schema.json}, which exists to have one property per
 * feature a submission can trip over.
 */
class SchemaEnforcementTest {

  private static final String PROCESS = "ValidationTest";

  private ProcessEngine engine;
  private String taskId;

  @BeforeEach
  void start() {
    engine = configuration().buildProcessEngine();
    engine.getRepositoryService().createDeployment()
        .addClasspathResource("validation.bpmn")
        .addClasspathResource("forms/Validation.schema.json")
        .addClasspathResource("forms/Validation.json")
        .deploy();

    ProcessInstance instance = engine.getRuntimeService().startProcessInstanceByKey(PROCESS);
    Task task = engine.getTaskService().createTaskQuery()
        .processInstanceId(instance.getId()).singleResult();
    assertNotNull(task, "the process should be waiting on the user task");
    taskId = task.getId();
  }

  @AfterEach
  void tearDown() {
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  // ---- readOnly and writeOnly -------------------------------------------------------------

  /**
   * A submission is a write, so a property the schema marks {@code readOnly} may be shown and
   * may not be sent back. This is the rule that defends a process's own identifiers from being
   * rewritten by whoever can reach the form.
   */
  @Test
  void aReadOnlyPropertyMayNotBeSubmitted() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("subjectId", "somebody-else")));

    assertEquals(List.of("/subjectId"), paths(refusal));
  }

  /** The same rule on the other route, so neither way in is the soft one. */
  @Test
  void aReadOnlyPropertyMayNotBeSubmittedThroughTheFormServiceEither() {
    assertThrows(JsonFormsValidatorException.class,
        () -> submitForm(submission().with("subjectId", "somebody-else")));

    assertNotNull(task(), "the task must still be open after a refused submission");
  }

  /** Leaving it out is the normal case and has to stay quiet. */
  @Test
  void aFormThatSimplyDoesNotSendTheReadOnlyPropertyIsAccepted() {
    complete(submission());

    assertNull(task());
  }

  /**
   * {@code writeOnly} is the mirror of {@code readOnly} and must not be read as a second way of
   * saying the same thing: it forbids the property in what is <em>returned</em>, and a
   * submission is exactly where it is allowed to appear. A validator that refuses it here would
   * make every password field unfillable.
   */
  @Test
  void aWriteOnlyPropertyMayBeSubmitted() {
    complete(submission().with("secret", "hunter2"));

    assertNull(task());
  }

  // ---- what a Camunda variable actually arrives as -----------------------------------------

  /**
   * Camunda hands integers over as {@code Long}, never {@code Integer}, and a validator that
   * insists on the narrower type refuses every number a real engine submits.
   */
  @Test
  void anIntegerSubmittedAsALongSatisfiesTypeInteger() {
    complete(submission().with("count", 42L));

    assertNull(task());
  }

  /** And the type still means something. */
  @Test
  void aStringWhereTheSchemaAsksForAnIntegerIsRefused() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("count", "many")));

    assertEquals(List.of("/count"), paths(refusal));
  }

  /**
   * Pins today's answer for a submitted null, which is a decision and not an obvious one.
   *
   * A null variable is dropped rather than validated, so the property counts as absent: a
   * required one is then missing, and an optional one is simply not there. Whatever replaces
   * the validator has to make the same choice explicitly - the alternative reading, that null
   * is a value of the wrong type, refuses forms that today clear a field by sending null.
   */
  @Test
  void aNullIsTreatedAsTheAbsenceOfTheProperty() {
    Map<String, Object> submitted = submission().build();
    submitted.put("notes", null);

    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submitted));

    assertTrue(refusal.getMessage().contains("notes"), refusal.getMessage());
  }

  /** An optional property sent as null is not a violation, for the same reason. */
  @Test
  void anOptionalPropertySentAsNullIsAccepted() {
    Map<String, Object> submitted = submission().build();
    submitted.put("count", null);

    complete(submitted);

    assertNull(task());
  }

  /**
   * A file upload arrives as a stream, and the schema calls it a string. The validator drops
   * the declared type for a {@code format: binary} property rather than refuse the stream for
   * not being text - without which no form with an upload on it could be submitted at all.
   */
  @Test
  void aStreamSatisfiesAStringDeclaredAsBinary() {
    complete(submission().with("attachment",
        new ByteArrayInputStream("a file".getBytes())));

    assertNull(task());
  }

  // ---- the shape of the data ---------------------------------------------------------------

  /**
   * Structured values are understood as Spin JSON and as nothing else.
   *
   * A plain {@code Map} is refused for not being the JSON library's own object type, whatever it
   * contains - so this is not a rule about the data but about how it arrived. It is pinned
   * because it is a sharp edge and because a validator built on a different JSON library would
   * very likely accept a {@code Map} instead, which is a behaviour change worth making on
   * purpose rather than discovering.
   */
  @Test
  void aNestedObjectSubmittedAsAPlainMapIsRefused() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("details", Map.of("code", "ABC"))));

    assertEquals(List.of("/details"), paths(refusal));
    assertEquals("type", refusal.toJsonFormsErrors().iterator().next().getKeyword());
  }

  /** The same for a list. */
  @Test
  void anArraySubmittedAsAPlainListIsRefused() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("tags", List.of("a", "b"))));

    assertEquals(List.of("/tags"), paths(refusal));
  }

  /** A rule inside a nested object reports the nested path, or it lands on no control at all. */
  @Test
  void aViolationInsideANestedObjectIsReportedAgainstTheNestedPath() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("details", json("{\"code\": \"abc\"}"))));

    assertEquals(List.of("/details/code"), paths(refusal));
  }

  /**
   * And a nested object that satisfies its subschema is not the thing refused.
   *
   * Asserted by spoiling something else and finding only that in the answer, because a
   * submission that passes outright goes on to be stored, and how the engine stores a Spin value
   * is a different subject from what the schema makes of it.
   */
  @Test
  void aWellFormedNestedObjectSatisfiesItsSubschema() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission()
            .with("details", json("{\"code\": \"ABC\"}"))
            .with("count", "many")));

    assertEquals(List.of("/count"), paths(refusal));
  }

  /** Arrays are validated per item, and the item's position is part of the path. */
  @Test
  void anArrayItemOfTheWrongTypeIsReportedAgainstItsIndex() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("tags", json("[\"a\", 2]"))));

    assertEquals(List.of("/tags/1"), paths(refusal));
  }

  /** Nothing the schema does not declare gets in. */
  @Test
  void anUndeclaredPropertyIsRefused() {
    assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("smuggled", "value")));
  }

  // ---- formats -----------------------------------------------------------------------------

  /**
   * {@code format: password} is a hint to the renderer about how to display a field, not a rule
   * about its contents, and the validator is configured to treat it as one. A replacement that
   * enforces unknown formats by default would refuse every password ever typed.
   */
  @Test
  void thePasswordFormatConstrainsNothing() {
    complete(submission().with("password", "anything at all !@#"));

    assertNull(task());
  }

  /** Pins whether a format with real semantics is enforced, which is a per-library default. */
  @Test
  void theEmailFormatIsEnforced() {
    assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("email", "not-an-email")));
  }

  // ---- what the client is given -------------------------------------------------------------

  /**
   * The error contract, which is what a JsonForms client binds to: an AJV-shaped
   * {@code instancePath} with no leading {@code #}, a keyword, and text to show. A path that
   * arrives in any other shape lands the error on no control, which looks to the person filling
   * the form like a refusal with no reason.
   */
  @Test
  void everyErrorCarriesAPathAKeywordAndSomethingToSay() {
    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submission().with("details", json("{\"code\": \"abc\"}"))));

    Collection<JsonFormsErrorObject> errors = refusal.toJsonFormsErrors();
    assertFalse(errors.isEmpty(), "a refusal must always carry at least one error");
    for (JsonFormsErrorObject error : errors) {
      assertNotNull(error.getKeyword(), "no keyword on " + error);
      assertNotNull(error.getInstancePath(), "no instance path on " + error);
      assertFalse(error.getInstancePath().startsWith("#"),
          "an instance path is AJV's, not a JSON pointer fragment: " + error.getInstancePath());
      assertFalse(error.getMessage() == null || error.getMessage().isBlank(),
          "an error with no text renders as a marker with no reason: " + error);
    }
  }

  /** A missing required property is reported against the object that should have held it. */
  @Test
  void aMissingRequiredPropertyNamesTheProperty() {
    Map<String, Object> submitted = new HashMap<>();

    JsonFormsValidatorException refusal = assertThrows(JsonFormsValidatorException.class,
        () -> complete(submitted));

    assertEquals(List.of(""), paths(refusal));
    assertTrue(refusal.getMessage().contains("notes"), refusal.getMessage());
  }

  // ---- fixture -------------------------------------------------------------------------------

  private void complete(Map<String, Object> submitted) {
    engine.getTaskService().complete(taskId, submitted);
  }

  private void complete(Submission submitted) {
    complete(submitted.build());
  }

  private void submitForm(Submission submitted) {
    engine.getFormService().submitTaskForm(taskId, submitted.build());
  }

  private Task task() {
    return engine.getTaskService().createTaskQuery().singleResult();
  }

  private static List<String> paths(JsonFormsValidatorException refusal) {
    List<String> paths = new ArrayList<>();
    refusal.toJsonFormsErrors().forEach(error -> paths.add(error.getInstancePath()));
    return paths;
  }

  /** A structured value as the engine hands one over: Spin JSON. */
  private static SpinJsonNode json(String value) {
    return Spin.JSON(value);
  }

  /** The smallest submission the form accepts, to be spoiled one property at a time. */
  private static Submission submission() {
    return new Submission().with("notes", "as agreed");
  }

  private static final class Submission {
    private final Map<String, Object> values = new HashMap<>();

    Submission with(String name, Object value) {
      values.put(name, value);
      return this;
    }

    Map<String, Object> build() {
      return new HashMap<>(values);
    }
  }

  private ProcessEngineConfigurationImpl configuration() {
    StandaloneInMemProcessEngineConfiguration configuration =
        new StandaloneInMemProcessEngineConfiguration();
    configuration.setJdbcUrl("jdbc:h2:mem:schema-" + System.nanoTime()
        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    configuration.setProcessEngineName("schema-" + System.nanoTime());
    configuration.setJobExecutorActivate(false);

    Map<Object, Object> beans = new HashMap<>();
    beans.put("jsonFormsValidator", new DefaultJsonFormsValidator(null));
    configuration.setBeans(beans);

    List<ProcessEnginePlugin> plugins = new ArrayList<>(Arrays.asList(
        new JsonFormsParseListenerProcessEnginePlugin(), new JsonFormsTaskServicePlugin()));
    configuration.setProcessEnginePlugins(plugins);

    return configuration;
  }
}
