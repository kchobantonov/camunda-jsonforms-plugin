package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading a form off disk instead of out of the deployment.
 *
 * The cases worth pinning are the ones a form key can produce: the deployment segment that tells
 * two archives' forms of the same name apart, a name that tries to leave the folder, and the
 * agreement between the two entry points - a {@code ?path=} URL and a deployment resource name
 * have to reach the same file, or a form renders from one copy and completes against another.
 */
class JsonFormsFolderResourcesTest {

  private static final String MOUNT = "/forms/" + Utils.CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER;
  private static final String SCHEMA = "forms/Start" + Utils.RESOURCE_SCHEMA_SUFFIX;

  @TempDir
  Path folder;

  /** One form resource under the folder of the archive that deploys it. */
  private void form(String archive, String resourceName, String content) throws IOException {
    Path resource = folder.resolve(archive).resolve(resourceName);
    Files.createDirectories(resource.getParent());
    Files.writeString(resource, content);
  }

  private String read(InputStream stream) throws IOException {
    assertNotNull(stream);
    try (InputStream in = stream) {
      return new String(in.readAllBytes());
    }
  }

  @Test
  void aPathFormKeyIsReadFromTheFolderItNames() throws IOException {
    form("Orders", SCHEMA, "{\"title\":\"start\"}");

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertTrue(resources.isActive());
    assertTrue(resources.isDeploymentScoped());
    assertEquals("{\"title\":\"start\"}",
        read(resources.resolve("/forms/Orders/forms/Start.schema.json")));
  }

  /**
   * The whole point of the deployment segment: the same location in two archives is two forms,
   * and each path reaches its own. Asserted through both entry points, since a caller reading a
   * form itself has to land on the same file the browser fetches.
   */
  @Test
  void twoArchivesHoldingTheSameLocationAreToldApart() throws IOException {
    form("Orders", SCHEMA, "{\"title\":\"one\"}");
    form("Invoices", SCHEMA, "{\"title\":\"two\"}");

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertEquals("{\"title\":\"one\"}", read(resources.resolve("/forms/Orders/" + SCHEMA)));
    assertEquals("{\"title\":\"two\"}", read(resources.resolve("/forms/Invoices/" + SCHEMA)));
    assertEquals("{\"title\":\"one\"}", read(resources.open("Orders", SCHEMA)));
    assertEquals("{\"title\":\"two\"}", read(resources.open("Invoices", SCHEMA)));
  }

  /**
   * The path the library itself builds has to be the path this reads, so the two are exercised
   * against each other rather than against a hand-written string.
   */
  @Test
  void thePathTheLibraryBuildsIsThePathThisReads() throws IOException {
    form("Orders", SCHEMA, "{}");

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertNotNull(resources.resolve(Utils.toPathLocation(MOUNT, "Orders", SCHEMA)));
  }

  /** No deployment segment, no form: the path would otherwise address every archive at once. */
  @Test
  void aPathWithoutTheDeploymentSegmentResolvesToNothing() throws IOException {
    form("Orders", SCHEMA, "{}");

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertNull(resources.resolve("/forms/" + SCHEMA));
    assertNull(resources.open(null, SCHEMA));
    assertNull(resources.open("", SCHEMA));
  }

  /** A mount naming no deployment reads the folder itself - one archive, its own folder. */
  @Test
  void aMountWithoutThePlaceholderReadsTheFolderItself() throws IOException {
    Files.createDirectories(folder.resolve("forms"));
    Files.writeString(folder.resolve(SCHEMA), "{\"title\":\"only\"}");

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), "/forms");

    assertFalse(resources.isDeploymentScoped());
    assertEquals("{\"title\":\"only\"}", read(resources.resolve("/forms/" + SCHEMA)));
    assertEquals("{\"title\":\"only\"}", read(resources.open(null, SCHEMA)));
    // the mount is a whole path segment, so a longer one that merely starts with it is not it
    assertNull(resources.resolve("/formsomething/" + SCHEMA));
  }

  @Test
  void nothingOutsideTheFolderCanBeReached() throws IOException {
    Path secret = folder.getParent().resolve("secret.json");
    Files.writeString(secret, "nope");
    Files.createDirectories(folder.resolve("Orders"));

    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertNull(resources.resolve("/forms/Orders/../../secret.json"));
    assertNull(resources.open("Orders", "../../secret.json"));
    assertNull(resources.open("Orders", secret.toString()));
    // the deployment segment names a folder, so it must not walk out either
    assertNull(resources.open("../..", "secret.json"));
    assertNull(resources.resolve("/forms/../../secret.json"));
  }

  /** A resource the folder does not hold is not an error; the caller falls back on it. */
  @Test
  void aResourceTheFolderDoesNotHoldResolvesToNull() {
    JsonFormsFolderResources resources = JsonFormsFolderResources.of(folder.toString(), MOUNT);

    assertNull(resources.resolve("/forms/Orders/forms/Absent.schema.json"));
    assertNull(resources.open("Orders", "forms/Absent.schema.json"));
  }

  /**
   * A mount that is not a URL path, or a folder that is not a folder, switches the override off
   * rather than half on - the same test {@link Utils#toPathLocation} applies before it builds a
   * path at all.
   */
  @Test
  void anUnusableSettingLeavesEveryReadToTheDeployment() throws IOException {
    form("Orders", SCHEMA, "{}");

    assertFalse(JsonFormsFolderResources.of(folder.toString(), "forms").isActive());
    assertFalse(JsonFormsFolderResources.of(folder.toString(), "/").isActive());
    assertFalse(JsonFormsFolderResources.of(folder.resolve("absent").toString(), MOUNT).isActive());
    assertFalse(JsonFormsFolderResources.of(null, MOUNT).isActive());
    assertFalse(JsonFormsFolderResources.off().isActive());

    JsonFormsFolderResources off = JsonFormsFolderResources.off();
    assertNull(off.resolve("/forms/Orders/" + SCHEMA));
    assertNull(off.open("Orders", SCHEMA));
    assertTrue(off.mounts().isEmpty());
    assertFalse(off.isDeploymentScoped());
  }

  /**
   * What an application serves as static resources. One entry per archive folder, each already
   * carrying the deployment name, so the browser fetching a rewritten form key reads the same
   * files as the server side.
   */
  @Test
  void everyArchiveFolderIsOfferedForServing() throws IOException {
    form("Orders", SCHEMA, "{}");
    form("Invoices", SCHEMA, "{}");
    Files.writeString(folder.resolve("notAFolder.json"), "{}");

    List<JsonFormsFolderResources.Mount> mounts =
        JsonFormsFolderResources.of(folder.toString(), MOUNT).mounts();

    assertEquals(List.of("/forms/Invoices", "/forms/Orders"),
        mounts.stream().map(JsonFormsFolderResources.Mount::pattern).toList());
    assertEquals(folder.resolve("Invoices").toUri().toURL().toExternalForm(),
        mounts.get(0).location());
  }

  /** A mount with no placeholder needs one handler, on the folder itself. */
  @Test
  void anUnscopedMountIsOfferedAsOneHandler() throws IOException {
    List<JsonFormsFolderResources.Mount> mounts =
        JsonFormsFolderResources.of(folder.toString(), "/forms").mounts();

    assertEquals(1, mounts.size());
    assertEquals("/forms", mounts.get(0).pattern());
    assertEquals(folder.toUri().toURL().toExternalForm(), mounts.get(0).location());
  }

  /** The two JVM options, which is how a running application configures this. */
  @Test
  void theSystemPropertiesConfigureIt() throws IOException {
    form("Orders", SCHEMA, "{\"title\":\"start\"}");
    System.setProperty(Utils.CAMUNDA_JSONFORMS_RESOURCES_FOLDER, folder.toString());
    System.setProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH, MOUNT);

    try {
      JsonFormsFolderResources resources = JsonFormsFolderResources.fromSystemProperties();

      assertTrue(resources.isActive());
      assertEquals("{\"title\":\"start\"}", read(resources.open("Orders", SCHEMA)));
    } finally {
      System.clearProperty(Utils.CAMUNDA_JSONFORMS_RESOURCES_FOLDER);
      System.clearProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);
    }
  }
}
