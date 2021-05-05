package de.upb.swt.soot.test.java.bytecode.minimaltestsuite.java6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.upb.swt.soot.core.jimple.common.constant.BooleanConstant;
import de.upb.swt.soot.core.jimple.common.constant.Constant;
import de.upb.swt.soot.core.jimple.common.constant.IntConstant;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.signatures.PackageName;
import de.upb.swt.soot.java.core.AnnotationUsage;
import de.upb.swt.soot.java.core.JavaIdentifierFactory;
import de.upb.swt.soot.java.core.JavaSootClass;
import de.upb.swt.soot.java.core.JavaSootField;
import de.upb.swt.soot.java.core.JavaSootMethod;
import de.upb.swt.soot.java.core.jimple.basic.JavaLocal;
import de.upb.swt.soot.java.core.language.JavaJimple;
import de.upb.swt.soot.java.core.types.AnnotationType;
import de.upb.swt.soot.test.java.bytecode.minimaltestsuite.MinimalBytecodeTestSuiteBase;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public class AnnotationUsageTest extends MinimalBytecodeTestSuiteBase {

  // we can only read: RetentionPolicy.RUNTIME annotations

  @Test
  public void testAnnotationOnClassOrAnnotation() {
    // ElementType.ANNOTATION_TYPE can be applied to an annotation type.
    JavaSootClass sootClass = loadClass(getDeclaredClassSignature());

    Map<String, Constant> annotationParamMap = new HashMap<>();
    annotationParamMap.put("sthBlue", IntConstant.getInstance(42));
    annotationParamMap.put("author", JavaJimple.getInstance().newStringConstant("GeorgeLucas"));

    assertEquals(
        Arrays.asList(
            new AnnotationUsage(
                new AnnotationType("NonInheritableOnClass", new PackageName(""), false),
                Collections.emptyMap()),
            new AnnotationUsage(
                new AnnotationType("OnClass", new PackageName(""), true), annotationParamMap)),
        sootClass.getAnnotations(Optional.of(customTestWatcher.getJavaView())));
  }

  @Test
  public void testAnnotationOnField() {
    // ElementType.FIELD can be applied to a field or property.
    JavaSootClass sootClass = loadClass(getDeclaredClassSignature());
    final Optional<JavaSootField> agent = sootClass.getField("agent");
    assertTrue(agent.isPresent());

    Map<String, Constant> annotationParamMap = new HashMap<>();
    annotationParamMap.put("isRipe", JavaJimple.getInstance().newStringConstant("true"));

    assertEquals(
        Collections.singletonList(
            new AnnotationUsage(
                new AnnotationType("OnField", new PackageName(""), false), annotationParamMap)),
        agent.get().getAnnotations());
  }

  @Test
  public void testAnnotationOnMethod() {
    // ElementType.METHOD can be applied to a method-level annotation.
    {
      JavaSootClass sootClass = loadClass(getDeclaredClassSignature());
      final Optional<JavaSootMethod> someMethod =
          sootClass.getMethod(
              JavaIdentifierFactory.getInstance()
                  .getMethodSignature(
                      "someMethod",
                      sootClass.getType(),
                      "void",
                      Arrays.asList("int", "boolean", "int", "boolean")));
      assertTrue(someMethod.isPresent());

      assertEquals(
          Collections.singletonList(
              new AnnotationUsage(
                  new AnnotationType("OnMethod", new PackageName(""), false),
                  Collections.emptyMap())),
          someMethod.get().getAnnotations());
    }

    // ElementType.CONSTRUCTOR can be applied to a constructor.
    {
      JavaSootClass sootClass = loadClass(getDeclaredClassSignature());
      final Optional<JavaSootMethod> someMethod =
          sootClass.getMethod("<init>", Collections.emptyList());
      assertTrue(someMethod.isPresent());

      Map<String, Constant> annotationParamMap = new HashMap<>();
      annotationParamMap.put("countOnMe", IntConstant.getInstance(1));
      Map<String, Constant> annotationParamMap2 = new HashMap<>();
      annotationParamMap2.put("countOnMe", IntConstant.getInstance(2));

      assertEquals(
          Arrays.asList(
              new AnnotationUsage(
                  new AnnotationType("OnMethodRepeatable", new PackageName(""), false),
                  annotationParamMap),
              new AnnotationUsage(
                  new AnnotationType("OnMethodRepeatable", new PackageName(""), false),
                  annotationParamMap2)),
          someMethod.get().getAnnotations());
    }
  }

  @Test
  public void testAnnotationOnLocal() {
    // ElementType.LOCAL_VARIABLE can be applied to a local variable. -> per JLS 9.6.4.2 this
    // information is not contained in bytecode
    // ElementType.PARAMETER can be applied to the parameters of a method.

    {
      JavaSootClass sootClass = loadClass(getDeclaredClassSignature());
      final Optional<JavaSootMethod> someMethod =
          sootClass.getMethod(
              JavaIdentifierFactory.getInstance()
                  .getMethodSignature(
                      "someMethod",
                      sootClass.getType(),
                      "void",
                      Arrays.asList("int", "boolean", "int", "boolean")));
      assertTrue(someMethod.isPresent());
      Body body = someMethod.get().getBody();
      assert body != null;
      JavaLocal parameterLocal = (JavaLocal) body.getParameterLocal(0);

      // parameter local annotation
      // int
      assertEquals(Collections.emptyList(), parameterLocal.getAnnotations());

      parameterLocal = (JavaLocal) body.getParameterLocal(1);
      // boolean with default annotation
      assertEquals(
          Collections.singletonList(
              new AnnotationUsage(
                  new AnnotationType("OnParameter", new PackageName(""), false),
                  Collections.emptyMap())),
          parameterLocal.getAnnotations());

      parameterLocal = (JavaLocal) body.getParameterLocal(2);
      // int
      assertEquals(Collections.emptyList(), parameterLocal.getAnnotations());

      parameterLocal = (JavaLocal) body.getParameterLocal(3);
      // boolean with annotation with custom value
      Map<String, Constant> annotationParamMap = new HashMap<>();
      annotationParamMap.put("isBigDuck", BooleanConstant.getTrue());
      assertEquals(
          Collections.singletonList(
              new AnnotationUsage(
                  new AnnotationType("OnParameter", new PackageName(""), false),
                  annotationParamMap)),
          parameterLocal.getAnnotations());
    }
  }
}