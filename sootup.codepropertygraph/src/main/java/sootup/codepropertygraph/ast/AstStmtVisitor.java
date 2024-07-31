package sootup.codepropertygraph.ast;

import javax.annotation.Nonnull;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.edges.*;
import sootup.codepropertygraph.propertygraph.nodes.*;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.Expr;
import sootup.core.jimple.common.ref.Ref;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.JEnterMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JExitMonitorStmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;
import sootup.core.jimple.visitor.AbstractStmtVisitor;

/** Visitor for statements in the AST. */
class AstStmtVisitor extends AbstractStmtVisitor<Void> {
  private final PropertyGraph.Builder graphBuilder;
  private final PropertyGraphNode parentNode;

  /**
   * Constructs an AST statement visitor.
   *
   * @param graphBuilder the property graph builder
   * @param parentNode the parent node
   */
  AstStmtVisitor(PropertyGraph.Builder graphBuilder, PropertyGraphNode parentNode) {
    this.graphBuilder = graphBuilder;
    this.parentNode = parentNode;
  }

  @Override
  public void caseAssignStmt(@Nonnull JAssignStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    PropertyGraphNode leftOpNode = createOperandNode(stmt.getLeftOp());
    graphBuilder.addEdge(new LeftOpAstEdge(stmtNode, leftOpNode));

    PropertyGraphNode rightOpNode = createOperandNode(stmt.getRightOp());
    graphBuilder.addEdge(new RightOpAstEdge(stmtNode, rightOpNode));
  }

  /**
   * Creates a property graph node for the given operand.
   *
   * @param operand the operand
   * @return the property graph node
   */
  private PropertyGraphNode createOperandNode(Object operand) {
    if (operand instanceof Immediate) {
      return new ImmediateGraphNode((Immediate) operand);
    } else if (operand instanceof Ref) {
      return new RefGraphNode((Ref) operand);
    } else if (operand instanceof Expr) {
      ExprGraphNode exprNode = new ExprGraphNode((Expr) operand);
      ((Expr) operand).accept(new AstExprVisitor(graphBuilder, exprNode));
      return exprNode;
    } else {
      throw new IllegalArgumentException("Unknown operand type: " + operand.getClass());
    }
  }

  @Override
  public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
    ExprGraphNode invokeExprNode = new ExprGraphNode(invokeExpr);
    graphBuilder.addEdge(new InvokeAstEdge(stmtNode, invokeExprNode));
    invokeExpr.accept(new AstExprVisitor(graphBuilder, invokeExprNode));
  }

  @Override
  public void caseReturnStmt(@Nonnull JReturnStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode opNode = new ImmediateGraphNode(stmt.getOp());
    graphBuilder.addEdge(new SingleOpAstEdge(stmtNode, opNode));
  }

  @Override
  public void caseIfStmt(@Nonnull JIfStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    AbstractConditionExpr condition = stmt.getCondition();
    ExprGraphNode exprNode = new ExprGraphNode(condition);
    graphBuilder.addEdge(new ConditionAstEdge(stmtNode, exprNode));
    condition.accept(new AstExprVisitor(graphBuilder, exprNode));
  }

  @Override
  public void caseNopStmt(@Nonnull JNopStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));
  }

  @Override
  public void caseThrowStmt(@Nonnull JThrowStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode opNode = new ImmediateGraphNode(stmt.getOp());
    graphBuilder.addEdge(new SingleOpAstEdge(stmtNode, opNode));
  }

  @Override
  public void caseIdentityStmt(@Nonnull JIdentityStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode leftOpNode = new ImmediateGraphNode(stmt.getLeftOp());
    graphBuilder.addEdge(new LeftOpAstEdge(stmtNode, leftOpNode));

    RefGraphNode rightOpNode = new RefGraphNode(stmt.getRightOp());
    graphBuilder.addEdge(new RightOpAstEdge(stmtNode, rightOpNode));
  }

  @Override
  public void caseGotoStmt(@Nonnull JGotoStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));
  }

  @Override
  public void caseEnterMonitorStmt(@Nonnull JEnterMonitorStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode opNode = new ImmediateGraphNode(stmt.getOp());
    graphBuilder.addEdge(new SingleOpAstEdge(stmtNode, opNode));
  }

  @Override
  public void caseExitMonitorStmt(@Nonnull JExitMonitorStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode opNode = new ImmediateGraphNode(stmt.getOp());
    graphBuilder.addEdge(new SingleOpAstEdge(stmtNode, opNode));
  }

  @Override
  public void caseSwitchStmt(@Nonnull JSwitchStmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));

    ImmediateGraphNode switchKeyNode = new ImmediateGraphNode(stmt.getKey());
    graphBuilder.addEdge(new SwitchKeyAstEdge(stmtNode, switchKeyNode));
  }

  @Override
  public void defaultCaseStmt(@Nonnull Stmt stmt) {
    StmtGraphNode stmtNode = new StmtGraphNode(stmt);
    graphBuilder.addEdge(new StmtAstEdge(parentNode, stmtNode));
  }
}
