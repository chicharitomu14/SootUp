package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java6;

import static org.junit.Assert.assertTrue;

import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** @author Kaustubh Kelkar */
public class InstanceOfCheckTest extends MinimalSourceTestSuiteBase {
  @Override
  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "instanceOfCheckMethod", getDeclaredClassSignature(), "void", Collections.emptyList());
  }

  @org.junit.Test
  public void defaultTest() {
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());
    SootClass sootClass = loadClass(getDeclaredClassSignature());
    assertTrue(sootClass.getSuperclass().get().getClassName().equals("InstanceOfCheckSuper"));
  }

  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: InstanceOfCheck",
            "$r1 = new InstanceOfCheck",
            "specialinvoke $r1.<InstanceOfCheck: void <init>()>()",
            "$r2 = <java.lang.System: java.io.PrintStream out>",
            "$z0 = $r1 instanceof InstanceOfCheckSuper",
            "virtualinvoke $r2.<java.io.PrintStream: void println(boolean)>($z0)",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}