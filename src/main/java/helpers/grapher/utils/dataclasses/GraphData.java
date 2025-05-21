package helpers.grapher.utils.dataclasses;

import java.util.ArrayList;
import java.util.List;

public class GraphData {
    private List<GraphNodeData> nodes = new ArrayList<>();
    private List<GraphEdgeData> edges = new ArrayList<>();

    // Getters and setters
    public List<GraphNodeData> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNodeData> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdgeData> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdgeData> edges) {
        this.edges = edges;
    }
}