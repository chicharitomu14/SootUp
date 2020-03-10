package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite;

import static org.junit.Assert.*;

import categories.Java8Test;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.frontend.AbstractClassSource;
import de.upb.swt.soot.core.model.*;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import de.upb.swt.soot.java.core.JavaProject;
import de.upb.swt.soot.java.core.language.JavaLanguage;
import de.upb.swt.soot.java.core.types.JavaClassType;
import de.upb.swt.soot.java.core.views.JavaView;
import de.upb.swt.soot.java.sourcecode.inputlocation.JavaSourcePathAnalysisInputLocation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * @author Markus Schmidt
 * @author Hasitha Rajapakse
 * @author Kaustubh Kelkar
 */
@Category(Java8Test.class)
public abstract class MinimalSourceTestSuiteBase {

  static final String baseDir = "../shared-test-resources/minimaltestsuite/";
  protected JavaIdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();

  @ClassRule public static CustomTestWatcher customTestWatcher = new CustomTestWatcher();

  public static class CustomTestWatcher extends TestWatcher {
    private String classPath = MinimalSourceTestSuiteBase.class.getSimpleName();
    private JavaView javaView;
    private JavaProject project;

    /** Load WalaClassLoader once for each test directory */
    @Override
    protected void starting(Description description) {
      String prevClassDirName = getTestDirectoryName(getClassPath());
      setClassPath(description.getClassName());
      if (!prevClassDirName.equals(getTestDirectoryName(getClassPath()))) {
        project =
            JavaProject.builder(new JavaLanguage(8))
                .addClassPath(
                    new JavaSourcePathAnalysisInputLocation(
                        baseDir
                            + File.separator
                            + getTestDirectoryName(getClassPath())
                            + File.separator
                            + "source"
                            + File.separator))
                .build();
        javaView = project.createOnDemandView();
        setJavaView(javaView);
      }
    }

    public String getClassPath() {
      return classPath;
    }

    private void setClassPath(String classPath) {
      this.classPath = classPath;
    }

    private void setJavaView(JavaView javaView) {
      this.javaView = javaView;
    }

    public JavaView getJavaView() {
      return javaView;
    }
  }

  public MethodSignature getMethodSignature() {
    fail("getMethodSignature() is used but not overridden");
    return null;
  }

  public List<String> expectedBodyStmts() {
    fail("expectedBodyStmts() is used but not overridden");
    return null;
  }

  /**
   * @returns the name of the parent directory - assuming the directory structure is only one level
   *     deep
   */
  public static String getTestDirectoryName(String classPath) {
    String[] classPathArray = classPath.split("\\.");
    String testDirectoryName = "";
    if (classPathArray.length > 1) {
      testDirectoryName = classPathArray[classPathArray.length - 2];
    }
    return testDirectoryName;
  }

  /**
   * @returns the name of the class - assuming the testname unit has "Test" appended to the
   *     respective name of the class
   */
  public String getClassName(String classPath) {
    String[] classPathArray = classPath.split("\\.");
    String className =
        classPathArray[classPathArray.length - 1].substring(
            0, classPathArray[classPathArray.length - 1].length() - 4);
    return className;
  }

  protected JavaClassType getDeclaredClassSignature() {
    return identifierFactory.getClassType(getClassName(customTestWatcher.classPath));
  }

  @Test
  public void defaultTest() {
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());
  }

  public SootClass loadClass(ClassType clazz) {
    Optional<AbstractClass<? extends AbstractClassSource>> cs =
        customTestWatcher.getJavaView().getClass(clazz);
    assertTrue("no matching class signature found", cs.isPresent());
    return (SootClass) cs.get();
  }

  public SootMethod loadMethod(MethodSignature methodSignature) {
    SootClass clazz = loadClass(methodSignature.getDeclClassType());
    Optional<SootMethod> m= clazz.getMethod(methodSignature);
    assertTrue("No matching method signature found", m.isPresent());
    SootMethod method = m.get();
    return method;
  }

  public void assertJimpleStmts(SootMethod method, List<String> expectedStmts) {
    Body body = method.getBody();
    assertNotNull(body);

    List<String> actualStmts = Utils.bodyStmtsAsStrings(body);
    assertEquals(expectedStmts, actualStmts);
  }

  public List<String> expectedBodyStmts(String... jimpleLines) {
    return Stream.of(jimpleLines).collect(Collectors.toList());
  }
}
