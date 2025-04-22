package helpers.grapher.utils;

import javafx.scene.shape.Line;

import java.util.Objects;

public class GraphEdge extends Line {
    private final GraphNode startNode;
    private final GraphNode endNode;

    public GraphEdge(GraphNode startNode, GraphNode endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

    public GraphNode getEndNode() {
        return endNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(startNode, graphEdge.startNode) && Objects.equals(endNode, graphEdge.endNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startNode, endNode);
    }
}
