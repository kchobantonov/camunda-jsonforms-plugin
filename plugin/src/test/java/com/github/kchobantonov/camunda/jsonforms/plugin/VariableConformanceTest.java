package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.rest.dto.VariableValueDto;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The UI's exact expected payloads, converted by Camunda REST DTOs and submitted to real BPMN tasks. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VariableConformanceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private ProcessEngine engine;
  private JsonNode spec;

  @BeforeAll
  void startEngine() throws Exception {
    try (InputStream stream = getClass().getResourceAsStream("/variable-spec/variable-mapping.json")) {
      spec = mapper.readTree(Objects.requireNonNull(stream));
    }
    var config = new StandaloneInMemProcessEngineConfiguration();
    config.setJdbcUrl("jdbc:h2:mem:variables-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    config.setProcessEngineName("variables-" + System.nanoTime());
    config.setJobExecutorActivate(false);
    config.setBeans(Map.of("jsonFormsValidator", new DefaultJsonFormsValidator(null)));
    config.setProcessEnginePlugins(List.of(new SpinProcessEnginePlugin(),
        new JsonFormsParseListenerProcessEnginePlugin(), new JsonFormsTaskServicePlugin()));
    engine = config.buildProcessEngine();
  }

  @AfterAll
  void closeEngine() { if (engine != null) engine.close(); }

  Stream<Arguments> writes() {
    return stream(spec.get("write")).flatMap(test -> Stream.of(false, true)
        .map(complete -> Arguments.of(test.get("name").asText(), complete, test)));
  }

  @ParameterizedTest(name = "{0}, complete={1}")
  @MethodSource("writes")
  void sharedPayloadsValidatePersistAndReadBack(String name, boolean complete, JsonNode test) throws Exception {
    String deployment = deploy(test.get("schema"));
    try {
      Task input = start("variableRoundTrip", deployment);
      Map<String, Object> variables = variables(test.get("variables"));
      submit(input, variables, complete);
      Task review = task(input.getProcessInstanceId());
      assertEquals("review", review.getTaskDefinitionKey());
      assertEquals(mapper.convertValue(test.get("variables"), Map.class).keySet(),
          engine.getRuntimeService().getVariables(input.getProcessInstanceId()).keySet(),
          "Root properties are variables; nested fields must not become separate variables");
      for (var entry : iterable(test.get("variables").fields())) {
        String variableName = entry.getKey();
        JsonNode expected = entry.getValue();
        TypedValue stored = engine.getRuntimeService().getVariableTyped(input.getProcessInstanceId(), variableName, false);
        VariableValueDto dto = VariableValueDto.fromTypedValue(stored);
        assertEquals(expected.get("type").asText(), dto.getType());
        if (stored instanceof FileValue) {
          FileValue file = (FileValue) stored;
          assertEquals("sample.txt", file.getFilename());
          assertEquals("text/plain", file.getMimeType());
          try (InputStream bytes = file.getValue()) {
            assertArrayEquals(Base64.getDecoder().decode(expected.get("value").asText()), bytes.readAllBytes());
          }
          assertNull(dto.getValue(), "form reads must not embed file bytes");
        } else if (expected.get("value").isNull()) {
          assertNull(dto.getValue());
        } else if (List.of("Json", "Object").contains(dto.getType())) {
          assertEquals(mapper.readTree(expected.get("value").asText()), mapper.readTree((String) dto.getValue()));
        } else {
          assertEquals(expected.get("value"), mapper.valueToTree(dto.getValue()));
        }
        for (var field : iterable(expected.get("valueInfo").fields())) {
          assertEquals(field.getValue(), mapper.valueToTree(dto.getValueInfo().get(field.getKey())));
        }
        if (test.has("javaClass")) {
          Object value = engine.getRuntimeService().getVariable(input.getProcessInstanceId(), variableName);
          assertEquals(test.get("javaClass").asText(), value.getClass().getName());
          if (test.has("itemClass")) {
            Collection<?> items = value instanceof Map ? ((Map<?, ?>) value).values() : (Collection<?>) value;
            for (Object item : items) assertEquals(test.get("itemClass").asText(), item.getClass().getName());
          }
          if (value instanceof LinkedHashSet) {
            assertEquals(mapper.convertValue(test.get("data").get(variableName), List.class), new ArrayList<>((Set<?>) value));
          }
        }
      }
    } finally { engine.getRepositoryService().deleteDeployment(deployment, true); }
  }

  Stream<Arguments> invalidSubmissions() {
    return stream(spec.get("reject")).flatMap(test -> Stream.of(false, true)
        .map(complete -> Arguments.of(test.get("name").asText(), complete, test)));
  }

  @ParameterizedTest(name = "{0}, complete={1}")
  @MethodSource("invalidSubmissions")
  void invalidSerializedValuesLeaveTheTaskAndVariablesUnchanged(String name, boolean complete, JsonNode test)
      throws Exception {
    String deployment = deploy(test.get("schema"));
    try {
      Task input = start("variableRoundTrip", deployment);
      Map<String, Object> values = variables(test.get("variables"));
      JsonFormsValidatorException exception = assertThrows(JsonFormsValidatorException.class,
          () -> submit(input, values, complete));
      String expectedPath = test.get("path").asText();
      assertTrue(exception.toJsonFormsErrors().stream().anyMatch(error ->
          expectedPath.equals(error.getInstancePath())),
          exception.getMessage());
      assertEquals(input.getId(), task(input.getProcessInstanceId()).getId());
      assertTrue(engine.getRuntimeService().getVariables(input.getProcessInstanceId()).isEmpty());
    } finally { engine.getRepositoryService().deleteDeployment(deployment, true); }
  }

  Stream<Arguments> malformedJson() {
    return Stream.of("Json", "Object").flatMap(type -> Stream.of("[", "[] trailing")
        .flatMap(value -> Stream.of(false, true).map(complete -> Arguments.of(type, value, complete))));
  }

  @ParameterizedTest
  @MethodSource("malformedJson")
  void malformedJsonIsAFieldValidationError(String type, String value, boolean complete) throws Exception {
    String deployment = deploy(mapper.readTree("""
        {"type":"object","properties":{"sample":{"type":"array","items":{"type":"string"}}}}
        """));
    try {
      Task input = start("variableRoundTrip", deployment);
      var dto = new VariableValueDto();
      dto.setType(type);
      dto.setValue(value);
      dto.setValueInfo(Map.of("objectTypeName", "java.util.ArrayList<java.lang.String>",
          "serializationDataFormat", "application/json"));
      Map<String, Object> values = Map.of("sample", dto.toTypedValue(engine, mapper));
      JsonFormsValidatorException exception = assertThrows(JsonFormsValidatorException.class,
          () -> submit(input, values, complete));
      assertEquals("/sample", exception.toJsonFormsErrors().iterator().next().getInstancePath());
      assertEquals(input.getId(), task(input.getProcessInstanceId()).getId());
    } finally { engine.getRepositoryService().deleteDeployment(deployment, true); }
  }

  Stream<Arguments> unsupportedFormats() {
    return stream(spec.get("unsupportedFormats")).flatMap(format -> Stream.of(false, true)
        .map(complete -> Arguments.of(format.asText(), complete)));
  }

  @ParameterizedTest
  @MethodSource("unsupportedFormats")
  void serializedObjectsOutsideJsonAreValidationErrors(String format, boolean complete) throws Exception {
    JsonNode schema = mapper.readTree("""
        {"type":"object","properties":{"sample":{"type":"array","items":{"type":"string"}}}}
        """);
    String deployment = deploy(schema);
    try {
      Task input = start("variableRoundTrip", deployment);
      var dto = new VariableValueDto();
      dto.setType("Object");
      dto.setValue("encoded content");
      dto.setValueInfo(Map.of("objectTypeName", "java.util.ArrayList", "serializationDataFormat", format));
      Map<String, Object> values = Map.of("sample", dto.toTypedValue(engine, mapper));
      JsonFormsValidatorException exception = assertThrows(JsonFormsValidatorException.class,
          () -> submit(input, values, complete));
      assertEquals("/sample", exception.toJsonFormsErrors().iterator().next().getInstancePath());
      assertEquals(input.getId(), task(input.getProcessInstanceId()).getId());
      assertFalse(engine.getRuntimeService().getVariables(input.getProcessInstanceId()).containsKey("sample"));
    } finally { engine.getRepositoryService().deleteDeployment(deployment, true); }
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void bpmnUsesCollectionAndSpinApisAndObservesTransientVariables(boolean complete) throws Exception {
    JsonNode schema = mapper.readTree("""
        {"type":"object","properties":{
          "tags":{"type":"array","items":{"type":"string"}},
          "settings":{"type":"object","properties":{"enabled":{"type":"boolean"}}},
          "action":{"type":"string"}},"required":["tags","settings","action"],"additionalProperties":false}
        """);
    String deployment = deploy(schema);
    try {
      Task input = start("variableExpressions", deployment);
      JsonNode payload = mapper.readTree("""
          {"tags":{"type":"Object","value":"[\\"alpha\\",\\"beta\\"]","valueInfo":{
             "objectTypeName":"java.util.ArrayList<java.lang.String>","serializationDataFormat":"application/json"}},
           "settings":{"type":"Json","value":"{\\"enabled\\":true}"},
           "action":{"type":"String","value":"confirm","valueInfo":{"transient":true}}}
          """);
      submit(input, variables(payload), complete);
      assertEquals("matched", task(input.getProcessInstanceId()).getTaskDefinitionKey());
      assertEquals("confirm", engine.getRuntimeService().getVariable(input.getProcessInstanceId(), "observedAction"));
      assertFalse(engine.getRuntimeService().getVariables(input.getProcessInstanceId()).containsKey("action"));
      assertEquals(0, engine.getHistoryService().createHistoricVariableInstanceQuery()
          .processInstanceId(input.getProcessInstanceId()).variableName("action").count());
    } finally { engine.getRepositoryService().deleteDeployment(deployment, true); }
  }

  private String deploy(JsonNode schema) {
    return engine.getRepositoryService().createDeployment().addClasspathResource("variable-roundtrip.bpmn")
        .addString("forms/Variables.schema.json", schema.toString()).deploy().getId();
  }

  private Task start(String key, String deployment) {
    var definition = engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment)
        .processDefinitionKey(key).singleResult();
    return task(engine.getRuntimeService().startProcessInstanceById(definition.getId()).getId());
  }

  private Task task(String instance) {
    return engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
  }

  private Map<String, Object> variables(JsonNode json) throws Exception {
    Map<String, Object> result = new HashMap<>();
    for (var field : iterable(json.fields())) {
      result.put(field.getKey(), mapper.treeToValue(field.getValue(), VariableValueDto.class).toTypedValue(engine, mapper));
    }
    return result;
  }

  private void submit(Task task, Map<String, Object> variables, boolean complete) {
    if (complete) engine.getTaskService().complete(task.getId(), variables);
    else engine.getFormService().submitTaskForm(task.getId(), variables);
  }

  private static <T> Iterable<T> iterable(Iterator<T> iterator) { return () -> iterator; }
  private static Stream<JsonNode> stream(JsonNode array) {
    return java.util.stream.StreamSupport.stream(array.spliterator(), false);
  }
}
