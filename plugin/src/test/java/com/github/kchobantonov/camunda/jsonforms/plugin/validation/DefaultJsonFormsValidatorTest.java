package com.github.kchobantonov.camunda.jsonforms.plugin.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.everit.json.schema.ReadWriteContext;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsErrorObject;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsValidatorException;

/**
 * What a refused submission tells the person who made it.
 *
 * A form whose requirements depend on which action was submitted is an {@code allOf} of
 * {@code if}/{@code then} - the shape every conditional form takes - and a combinator reports
 * itself, not the field. everit's violation tree for a missing phone number is four deep:
 *
 * <pre>
 * allOf     #                   "only 1 subschema matches out of 2"
 *   allOf   #                   "only 1 subschema matches out of 2"
 *     then  #                   "input is invalid against the then schema"
 *       required #/attestation  "required key [phone] not found"
 * </pre>
 *
 * Only the last line is usable. Reporting the root and one level below it - which is what this
 * class used to do - answered with the same combinator message twice and dropped the rest, so
 * a form that needed a phone number came back saying "only 1 subschema matches out of 2".
 */
class DefaultJsonFormsValidatorTest {

    /** The shape of a form that asks for different things depending on the action taken. */
    private static final String CONDITIONAL_FORM = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "attestation": {
                  "type": "object",
                  "properties": {
                    "confirmed": { "type": "boolean" },
                    "phone": { "type": "string" }
                  }
                },
                "reason": { "type": "string" }
              },
              "patternProperties": {
                "taskActionName": { "type": "string", "enum": ["approve", "reject"] }
              },
              "allOf": [
                {
                  "if": { "required": ["taskActionName"],
                          "properties": { "taskActionName": { "const": "approve" } } },
                  "then": {
                    "required": ["attestation"],
                    "properties": {
                      "attestation": {
                        "required": ["confirmed", "phone"],
                        "properties": { "confirmed": { "const": true },
                                        "phone": { "pattern": "^[0-9]{10}$" } }
                      }
                    }
                  }
                },
                {
                  "if": { "required": ["taskActionName"],
                          "properties": { "taskActionName": { "const": "reject" } } },
                  "then": { "required": ["reason"] }
                }
              ],
              "additionalProperties": false
            }""";

    @Test
    void aMissingFieldIsReportedAgainstTheFieldAndNotAsACombinator() {
        List<JsonFormsErrorObject> errors = validate(
                "{\"taskActionName\": \"approve\", \"attestation\": {\"confirmed\": true}}");

        JsonFormsErrorObject error = only(errors);
        assertEquals("required", error.getKeyword());
        assertEquals("/attestation", error.getInstancePath());
        assertTrue(error.getMessage().contains("phone"), error.getMessage());
    }

    /** A violation deeper still keeps its own path, so it lands on the control that caused it. */
    @Test
    void aFailedRuleIsReportedAgainstTheExactProperty() {
        List<JsonFormsErrorObject> errors = validate(
                "{\"taskActionName\": \"approve\", \"attestation\":"
                        + " {\"confirmed\": true, \"phone\": \"555\"}}");

        JsonFormsErrorObject error = only(errors);
        assertEquals("pattern", error.getKeyword());
        assertEquals("/attestation/phone", error.getInstancePath());
    }

    /**
     * The path is what a JsonForms client matches its controls against, and it is AJV's
     * {@code instancePath} - no leading {@code #}, the document root as the empty string - so
     * an error on the whole object lands there rather than on a control called "#".
     */
    @Test
    void thePathIsTheOneAClientBindsTo() {
        List<JsonFormsErrorObject> errors = validate("{\"taskActionName\": \"reject\"}");

        JsonFormsErrorObject error = only(errors);
        assertEquals("required", error.getKeyword());
        assertEquals("", error.getInstancePath());
        assertTrue(error.getMessage().contains("reason"), error.getMessage());
    }

    /**
     * everit describes most violations and not all of them: a failed {@code const} arrives
     * with an empty message, which a client renders as an error marker with no text.
     */
    @Test
    void aViolationEveritDoesNotDescribeStillSaysSomething() {
        List<JsonFormsErrorObject> errors = validate(
                "{\"taskActionName\": \"approve\", \"attestation\":"
                        + " {\"confirmed\": false, \"phone\": \"5553304417\"}}");

        JsonFormsErrorObject error = only(errors);
        assertEquals("const", error.getKeyword());
        assertEquals("/attestation/confirmed", error.getInstancePath());
        assertFalse(error.getMessage() == null || error.getMessage().isBlank(),
                "a violation with no text of its own still has to say something");
    }

    /** Flattening never loses the failure itself, however shallow the tree. */
    @Test
    void aViolationWithNoCausesIsItsOwnLeaf() {
        List<JsonFormsErrorObject> errors = validate("{\"reason\": 7}");

        assertFalse(errors.isEmpty(), "a refusal must always carry at least one error");
        errors.forEach(error -> assertFalse(error.getKeyword() == null));
    }

    private static JsonFormsErrorObject only(List<JsonFormsErrorObject> errors) {
        assertEquals(1, errors.size(), "expected one error, got " + errors);
        return errors.get(0);
    }

    /** Runs the validator's own flattening over a real violation tree. */
    private static List<JsonFormsErrorObject> validate(String data) {
        Schema schema = SchemaLoader.builder()
                .schemaJson(new JSONObject(new JSONTokener(CONDITIONAL_FORM)))
                .draftV7Support().build().load().build();
        try {
            Validator.builder().failEarly().readWriteContext(ReadWriteContext.WRITE).build()
                    .performValidation(schema, new JSONObject(new JSONTokener(data)));
            throw new AssertionError("expected " + data + " to be refused");
        } catch (ValidationException e) {
            return List.copyOf(new DefaultJsonFormsValidator(null).toJsonFormsErrorObjects(e));
        }
    }
}
