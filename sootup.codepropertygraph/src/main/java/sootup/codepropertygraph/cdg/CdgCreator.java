package sootup.codepropertygraph.cdg;

import java.util.*;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.StmtMethodPropertyGraph;
import sootup.codepropertygraph.propertygraph.edges.CdgEdge;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.PostDominanceFinder;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;

/**
 * This class is responsible for creating the Control Dependence Graph (CDG) property graph for a given Soot method.
 */
public class CdgCreator {

  /**
   * Creates the CDG property graph for the given Soot method.
   *
   * @param method the Soot method
   * @return the CDG property graph
   */
  public PropertyGraph createGraph(SootMethod method) {
    PropertyGraph.Builder graphBuilder = new StmtMethodPropertyGraph.Builder();
    graphBuilder.setName("cdg_" + method.getName());

    if (method.isAbstract() || method.isNative()) {
      return graphBuilder.build();
    }

    StmtGraph<?> stmtGraph = method.getBody().getStmtGraph();
    PostDominanceFinder postDominanceFinder = new PostDominanceFinder(stmtGraph);

    List<? extends BasicBlock<?>> blocks = stmtGraph.getBlocksSorted();
    for (BasicBlock<?> currBlock : blocks) {
      for (BasicBlock<?> frontierBlock : postDominanceFinder.getDominanceFrontiers(currBlock)) {
        StmtGraphNode sourceNode = new StmtGraphNode(frontierBlock.getTail());
        for (Stmt srcStmt : currBlock.getStmts()) {
          StmtGraphNode destinationNode = new StmtGraphNode(srcStmt);
          graphBuilder.addEdge(new CdgEdge(sourceNode, destinationNode));
        }
      }
    }

    return graphBuilder.build();
  }
}
