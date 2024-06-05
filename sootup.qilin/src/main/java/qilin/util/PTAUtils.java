/* Qilin - a Java Pointer Analysis Framework
 * Copyright (C) 2021-2030 Qilin developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3.0 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <https://www.gnu.org/licenses/lgpl-3.0.en.html>.
 */

package qilin.util;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qilin.core.PTA;
import qilin.core.PTAScene;
import qilin.core.builder.MethodNodeFactory;
import qilin.core.builder.callgraph.Edge;
import qilin.core.context.Context;
import qilin.core.context.ContextElement;
import qilin.core.context.ContextElements;
import qilin.core.pag.*;
import qilin.core.sets.PointsToSet;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.PackageName;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.NullType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.core.util.printer.JimplePrinter;
import sootup.core.views.View;
import sootup.java.core.JavaIdentifierFactory;

public final class PTAUtils {
  private static final Logger logger = LoggerFactory.getLogger(PTAUtils.class);

  public static ClassType getClassType(String fullyQualifiedClassName) {
    return JavaIdentifierFactory.getInstance().getClassType(fullyQualifiedClassName);
  }

  public static boolean isStaticInitializer(SootMethod method) {
    return method.getName().equals("<clinit>");
  }

  public static boolean isConstructor(SootMethod method) {
    return method.getName().equals("<init>");
  }

  public static Map<LocalVarNode, Set<AllocNode>> calcStaticThisPTS(PTA pta) {
    Map<LocalVarNode, Set<AllocNode>> pts = new HashMap<>();
    Set<SootMethod> workList = new HashSet<>();
    PAG pag = pta.getPag();
    // add all instance methods which potentially contain static call
    for (SootMethod method : pta.getNakedReachableMethods()) {
      if (PTAUtils.isFakeMainMethod(method) || method.isConcrete() && !method.isStatic()) {
        MethodPAG srcmpag = pag.getMethodPAG(method);
        LocalVarNode thisRef = (LocalVarNode) srcmpag.nodeFactory().caseThis();
        final PointsToSet other = pta.reachingObjects(thisRef).toCIPointsToSet();

        for (final Stmt s : srcmpag.getInvokeStmts()) {
          AbstractInvokeExpr ie = s.getInvokeExpr();
          if (ie instanceof JStaticInvokeExpr) {
            for (Iterator<Edge> it = pta.getCallGraph().edgesOutOf(s); it.hasNext(); ) {
              Edge e = it.next();
              SootMethod tgtmtd = e.tgt();
              MethodPAG tgtmpag = pag.getMethodPAG(tgtmtd);
              MethodNodeFactory tgtnf = tgtmpag.nodeFactory();
              LocalVarNode tgtThisRef = (LocalVarNode) tgtnf.caseThis();
              // create "THIS" ptr for static method
              Set<AllocNode> addTo = pts.computeIfAbsent(tgtThisRef, k -> new HashSet<>());
              boolean returnValue = false;
              for (Iterator<AllocNode> itx = other.iterator(); itx.hasNext(); ) {
                AllocNode node = itx.next();
                if (addTo.add(node)) {
                  returnValue = true;
                }
              }
              if (returnValue) {
                workList.add(tgtmtd);
              }
            }
          }
        }
      }
    }
    while (!workList.isEmpty()) {
      SootMethod method = workList.iterator().next();
      workList.remove(method);
      MethodPAG srcmpag = pag.getMethodPAG(method);
      LocalVarNode thisRef = (LocalVarNode) srcmpag.nodeFactory().caseThis();
      final Set<AllocNode> other = pts.computeIfAbsent(thisRef, k -> new HashSet<>());

      for (final Stmt s : srcmpag.getInvokeStmts()) {
        AbstractInvokeExpr ie = s.getInvokeExpr();
        if (ie instanceof JStaticInvokeExpr) {
          for (Iterator<Edge> it = pta.getCallGraph().edgesOutOf(s); it.hasNext(); ) {
            Edge e = it.next();
            SootMethod tgtmtd = e.tgt();
            MethodPAG tgtmpag = pag.getMethodPAG(tgtmtd);
            MethodNodeFactory tgtnf = tgtmpag.nodeFactory();
            LocalVarNode tgtThisRef = (LocalVarNode) tgtnf.caseThis();
            // create "THIS" ptr for static method
            Set<AllocNode> addTo = pts.computeIfAbsent(tgtThisRef, k -> new HashSet<>());
            if (addTo.addAll(other)) {
              workList.add(tgtmtd);
            }
          }
        }
      }
    }
    return pts;
  }

  public static Object getIR(Object sparkNode) {
    if (sparkNode instanceof VarNode) {
      return ((VarNode) sparkNode).getVariable();
    } else if (sparkNode instanceof AllocNode) {
      return ((AllocNode) sparkNode).getNewExpr();
    } else { // sparkField?
      return sparkNode;
    }
  }

  public static boolean mustAlias(PTA pta, VarNode v1, VarNode v2) {
    PointsToSet v1pts = pta.reachingObjects(v1).toCIPointsToSet();
    PointsToSet v2pts = pta.reachingObjects(v2).toCIPointsToSet();
    return v1pts.pointsToSetEquals(v2pts);
  }

  public static void printPts(PTA pta, PointsToSet pts) {
    final StringBuffer ret = new StringBuffer();
    for (Iterator<AllocNode> it = pts.iterator(); it.hasNext(); ) {
      AllocNode n = it.next();
      ret.append("\t").append(n).append("\n");
    }
    System.out.print(ret);
  }

  public static String getNodeLabel(Node node) {
    int num = node.getNumber();
    if (node instanceof LocalVarNode) return "L" + num;
    else if (node instanceof ContextVarNode) {
      return "L" + num;
    } else if (node instanceof GlobalVarNode) return "G" + num;
    else if (node instanceof ContextField) return "OF" + num;
    else if (node instanceof FieldRefNode) return "VF" + num;
    else if (node instanceof AllocNode) return "O" + num;
    else throw new RuntimeException("no such node type exists!");
  }

  public static boolean isThrowable(View view, Type type) {
    if (type instanceof ClassType) {
      return canStoreType(view, type, PTAUtils.getClassType("java.lang.Throwable"));
    }
    return false;
  }

  public static boolean canStoreType(View view, final Type child, final Type parent) {
    if (child == parent || child.equals(parent)) {
      return true;
    }
    return view.getTypeHierarchy().isSubtype(parent, child);
  }

  public static boolean castNeverFails(View view, Type src, Type dst) {
    if (dst == null) return true;
    if (dst == src) return true;
    if (src == null) return false;
    if (dst.equals(src)) return true;
    if (src instanceof NullType) return true;
    if (dst instanceof NullType) return false;
    return canStoreType(view, src, dst);
  }

  public static boolean subtypeOfAbstractStringBuilder(Type t) {
    if (!(t instanceof ClassType)) {
      return false;
    }
    ClassType rt = (ClassType) t;
    String s = rt.toString();
    return (s.equals("java.lang.StringBuffer") || s.equals("java.lang.StringBuilder"));
  }

  public static Context plusplusOp(AllocNode heap) {
    ContextElement[] array;
    int s;
    if (heap instanceof ContextAllocNode) {
      ContextAllocNode csHeap = (ContextAllocNode) heap;
      ContextElements ctxElems = (ContextElements) csHeap.context();
      int ms = ctxElems.size();
      ContextElement[] oldArray = ctxElems.getElements();
      array = new ContextElement[ms + 1];
      array[0] = csHeap.base();
      System.arraycopy(oldArray, 0, array, 1, ms);
      s = ms + 1;
    } else {
      array = new ContextElement[1];
      array[0] = heap;
      s = 1;
    }
    return new ContextElements(array, s);
  }

  public static boolean isFakeMainMethod(SootMethod method) {
    String sig = "<qilin.pta.FakeMain: void main()>";
    return method.getSignature().toString().equals(sig);
  }

  public static boolean isFakeMainClass(ClassType classType) {
    String sig = "qilin.pta.FakeMain";
    return classType.toString().equals(sig);
  }

  public static boolean isOfPrimitiveBaseType(AllocNode heap) {
    if (heap.getType() instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) heap.getType();
      return arrayType.getBaseType() instanceof PrimitiveType;
    }
    return false;
  }

  public static boolean isPrimitiveArrayType(Type type) {
    if (type instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) type;
      return arrayType.getElementType() instanceof PrimitiveType;
    }
    return false;
  }

  public static void dumpJimple(PTAScene scene, String outputDir) {
    for (SootClass clz : scene.getLibraryClasses()) {
      PTAUtils.writeJimple(outputDir, clz);
    }
    for (SootClass clz : scene.getApplicationClasses()) {
      PTAUtils.writeJimple(outputDir, clz);
    }
  }

  /** Write the jimple file for clz. ParentDir is the absolute path of parent directory. */
  public static void writeJimple(String parentDir, SootClass clz) {
    PackageName pkgName = clz.getType().getPackageName();
    String clzName = clz.getType().getClassName();
    File packageDirectory =
        new File(parentDir + File.separator + pkgName.getName().replace(".", File.separator));

    try {
      packageDirectory.mkdirs();
      OutputStream streamOut =
          new FileOutputStream(packageDirectory + File.separator + clzName + ".jimple");
      PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
      new JimplePrinter().printTo(clz, writerOut);
      writerOut.flush();
      writerOut.close();
      streamOut.close();

    } catch (Exception e) {
      logger.error("Error writing jimple to file {}", clz, e);
    }
  }

  public static String findMainFromMetaInfo(String appPath) {
    String mainClass = null;
    try {
      JarFile jar = new JarFile(appPath);
      Enumeration<JarEntry> allEntries = jar.entries();
      while (allEntries.hasMoreElements()) {
        JarEntry entry = allEntries.nextElement();
        String name = entry.getName();
        if (!name.endsWith(".MF")) {
          continue;
        }
        String urlstring = "jar:file:" + appPath + "!/" + name;
        URL url = new URL(urlstring);
        Scanner scanner = new Scanner(url.openStream());
        while (scanner.hasNext()) {
          String string = scanner.next();
          if ("Main-Class:".equals(string) && scanner.hasNext()) {
            mainClass = scanner.next();
            break;
          }
        }
        if (mainClass == null) {
          System.out.println("cannot find meta info.");
        }
        scanner.close();
        jar.close();
        break;
      }
    } catch (IOException e) {
      System.out.println("cannot find meta info.");
    }
    return mainClass;
  }

  private static final Map<SootMethod, Body> methodToBody = DataFactory.createMap();

  public static Body getMethodBody(SootMethod m) {
    Body body = methodToBody.get(m);
    if (body == null) {
      if (m.isConcrete()) {
        body = m.getBody();
      } else {
        body = Body.builder().setMethodSignature(m.getSignature()).build();
      }
      methodToBody.putIfAbsent(m, body);
    }
    return body;
  }

  public static void updateMethodBody(SootMethod m, Body body) {
    methodToBody.put(m, body);
  }

  public static boolean isEmptyArray(AllocNode heap) {
    Object var = heap.getNewExpr();
    if (var instanceof JNewArrayExpr) {
      JNewArrayExpr nae = (JNewArrayExpr) var;
      Value sizeVal = nae.getSize();
      if (sizeVal instanceof IntConstant) {
        IntConstant size = (IntConstant) sizeVal;
        return size.getValue() == 0;
      }
    }
    return false;
  }

  public static LocalVarNode paramToArg(PAG pag, Stmt invokeStmt, MethodPAG srcmpag, VarNode pi) {
    MethodNodeFactory srcnf = srcmpag.nodeFactory();
    AbstractInvokeExpr ie = invokeStmt.getInvokeExpr();
    Parm mPi = (Parm) pi.getVariable();
    LocalVarNode thisRef = (LocalVarNode) srcnf.caseThis();
    LocalVarNode receiver;
    if (ie instanceof AbstractInstanceInvokeExpr) {
      AbstractInstanceInvokeExpr iie = (AbstractInstanceInvokeExpr) ie;
      Local base = iie.getBase();
      receiver = pag.findLocalVarNode(srcmpag.getMethod(), base, base.getType());
    } else {
      // static call
      receiver = thisRef;
    }
    if (mPi.isThis()) {
      return receiver;
    } else if (mPi.isReturn()) {
      if (invokeStmt instanceof JAssignStmt) {
        JAssignStmt assignStmt = (JAssignStmt) invokeStmt;
        Value mR = assignStmt.getLeftOp();
        return (LocalVarNode) pag.findValNode(mR);
      } else {
        return null;
      }
    } else if (mPi.isThrowRet()) {
      return srcnf.makeInvokeStmtThrowVarNode(invokeStmt, srcmpag.getMethod());
    }
    // Normal formal parameters.
    Value arg = ie.getArg(mPi.getIndex());
    if (arg == null) {
      return null;
    } else {
      return pag.findLocalVarNode(srcmpag.getMethod(), arg, arg.getType());
    }
  }
}
