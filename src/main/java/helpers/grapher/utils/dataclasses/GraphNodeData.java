package helpers.grapher.utils.dataclasses;

public class GraphNodeData {
    private String nodeId;
    private double x;
    private double y;
    private double z; // New Z field

    public GraphNodeData(String nodeId, double x, double y, double z) { // Updated constructor to include Z
        this.nodeId = nodeId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getters and setters
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }
}