package com.github.kchobantonov.camunda.jsonforms.spring;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Map;

import com.github.kchobantonov.camunda.jsonforms.plugin.*;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

/** Exercises the actual Spring condition, then the resulting engine behavior against deployed BPMN. */
class ValidateOnCompletePropertyTest {
  @ParameterizedTest(name = "validate-on-complete={0}")
  @ValueSource(strings = {"unset", "false", "true"})
  void propertyControlsTaskCompletionValidation(String setting) {
    try (var context = new AnnotationConfigApplicationContext()) {
      if (!"unset".equals(setting)) context.getEnvironment().getPropertySources().addFirst(
          new MapPropertySource("test", Map.of("camunda.jsonforms.validate-on-complete", setting)));
      context.registerBean(JsonFormsPathResourceResolver.class, () -> path -> null);
      context.register(JsonFormsPluginConfig.class);
      context.refresh();
      boolean enabled = "true".equals(setting);
      assertEquals(enabled ? 1 : 0, context.getBeansOfType(JsonFormsTaskServicePlugin.class).size());

      var config = new StandaloneInMemProcessEngineConfiguration();
      config.setJdbcUrl("jdbc:h2:mem:property-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
      config.setProcessEngineName("property-" + System.nanoTime());
      config.setJobExecutorActivate(false);
      config.setBeans(Map.of("jsonFormsValidator", context.getBean("jsonFormsValidator")));
      config.setProcessEnginePlugins(new ArrayList<>(context.getBeansOfType(ProcessEnginePlugin.class).values()));
      var engine = config.buildProcessEngine();
      try {
        engine.getRepositoryService().createDeployment().addClasspathResource("variable-roundtrip.bpmn")
            .addString("forms/Variables.schema.json", """
                {"type":"object","properties":{"label":{"type":"string","minLength":1}},
                 "required":["label"],"additionalProperties":false}
                """).deploy();
        for (String route : new String[] {"submit", "complete", "completeWithVariablesInReturn"}) {
          boolean complete = !"submit".equals(route);
          for (Map<String, Object> invalid : java.util.List.<Map<String, Object>>of(
              Map.of(), Map.of("label", ""), Map.of("label", 5), Map.of("label", "valid", "extra", true))) {
            String instance = engine.getRuntimeService().startProcessInstanceByKey("variableRoundTrip").getId();
            var input = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
            java.util.function.Consumer<Map<String, Object>> submit = values -> {
              if ("completeWithVariablesInReturn".equals(route)) {
                engine.getTaskService().completeWithVariablesInReturn(input.getId(), values, false);
              } else if (complete) engine.getTaskService().complete(input.getId(), values);
              else engine.getFormService().submitTaskForm(input.getId(), values);
            };
            if (enabled || !complete) {
              assertThrows(JsonFormsValidatorException.class, () -> submit.accept(invalid));
              assertEquals(input.getId(), engine.getTaskService().createTaskQuery()
                  .processInstanceId(instance).singleResult().getId());
              assertFalse(engine.getRuntimeService().getVariables(instance).containsKey("label"));
              // A refusal leaves the task usable: a corrected submission succeeds.
              submit.accept(Map.of("label", "valid"));
            } else {
              assertDoesNotThrow(() -> submit.accept(invalid));
            }
            var review = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
            assertEquals("review", review.getTaskDefinitionKey());
            // The enabled plugin does not impose validation on a task without a validator.
            engine.getTaskService().complete(review.getId(), Map.of("undeclared", true));
            assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
          }
        }
      } finally { engine.close(); }
    }
  }
}
