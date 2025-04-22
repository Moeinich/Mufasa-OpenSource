package helpers.grapher.utils.dataclasses;

public class GraphEdgeData {
    private String startNodeId;
    private String endNodeId;

    public GraphEdgeData(String startNodeId, String endNodeId) {
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
    }

    // Getters and setters
    public String getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId;
    }

    public String getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(String endNodeId) {
        this.endNodeId = endNodeId;
    }
}