package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * How a {@code ?deployment=} location becomes the URL a form is served under.
 *
 * The one thing worth pinning here is the deployment segment. A location is only unique within
 * its deployment, so with one process archive per process - each with its own
 * {@code resourceRootPath} - two archives can hold the same {@code forms/Start} and a path built
 * without the deployment name addresses both.
 */
class UtilsTest {

  private static final String LOCATION = "forms/Start";

  @Test
  void aMountWithNoPlaceholderIsUsedAsItIs() {
    assertEquals("/forms/forms/Start", Utils.toPathLocation("/forms", "Orders", LOCATION));
    assertEquals("/forms/forms/Start", Utils.toPathLocation("/forms/", "Orders", LOCATION));
    // no deployment is needed when the mount does not ask for one - the shape a single archive
    // with no resourceRootPath wants, since its locations are already full paths
    assertEquals("/forms/forms/Start", Utils.toPathLocation("/forms", null, LOCATION));
  }

  @Test
  void thePlaceholderPutsTheDeploymentNameInTheUrl() {
    assertEquals("/forms/Orders/forms/Start",
        Utils.toPathLocation("/forms/{deployment}", "Orders", LOCATION));
    // the same location in another archive, told apart
    assertEquals("/forms/Invoices/forms/Start",
        Utils.toPathLocation("/forms/{deployment}", "Invoices", LOCATION));
  }

  @Test
  void thePlaceholderNeedNotBeTheLastSegment() {
    assertEquals("/webjars/forms/Orders/local/forms/Start",
        Utils.toPathLocation("/webjars/forms/{deployment}/local", "Orders", LOCATION));
  }

  /**
   * Serving the path without the segment the mount asked for would address another archive's
   * form of the same name, so there is no path at all - which leaves the form key naming the
   * deployment and the form coming from there.
   */
  @Test
  void aPlaceholderWithNoDeploymentNameYieldsNoPath() {
    assertNull(Utils.toPathLocation("/forms/{deployment}", null, LOCATION));
    assertNull(Utils.toPathLocation("/forms/{deployment}", "", LOCATION));
  }

  /** The mount is a URL path; anything else is not a mount. */
  @Test
  void aMountThatIsNotAnAbsolutePathYieldsNoPath() {
    assertNull(Utils.toPathLocation("forms", "Orders", LOCATION));
    assertNull(Utils.toPathLocation(null, "Orders", LOCATION));
    assertNull(Utils.toPathLocation("/forms", "Orders", null));
  }

  /**
   * The two parameters are alternatives, and each is read on its own: a rewritten key carries
   * only {@code path}, and a BPMN-authored one only {@code deployment}.
   */
  @Test
  void aFormKeyNamesOneSourceOrTheOther() {
    String deployed = Utils.CAMUNDA_JSONFORMS_URL + "?deployment=forms/Start";
    assertEquals("forms/Start", Utils.getDeploymentLocation(deployed));
    assertNull(Utils.getPathLocation(deployed));

    String served = Utils.CAMUNDA_JSONFORMS_URL + "?path=/forms/Orders/forms/Start&debug=true";
    assertEquals("/forms/Orders/forms/Start", Utils.getPathLocation(served));
    assertNull(Utils.getDeploymentLocation(served));
  }
}
