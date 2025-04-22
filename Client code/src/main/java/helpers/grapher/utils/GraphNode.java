package helpers.grapher.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.shape.Rectangle;

import java.util.Objects;

public class GraphNode extends Rectangle {
    private final String nodeId;
    private final DoubleProperty xProperty = new SimpleDoubleProperty();
    private final DoubleProperty yProperty = new SimpleDoubleProperty();
    private final DoubleProperty zProperty = new SimpleDoubleProperty(); // New Z property for plane
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public GraphNode(String nodeId, double x, double y, double z) {
        this.xProperty.set(x);
        this.yProperty.set(y);
        this.zProperty.set(z);
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getNodeX() {
        return (int) xProperty.get();
    }

    public int getNodeY() {
        return (int) yProperty.get();
    }

    public int getNodeZ() { // New getter for Z
        return (int) zProperty.get();
    }

    public void setCoordinates(double x, double y, double z) {
        this.xProperty.set(x);
        this.yProperty.set(y);
        this.zProperty.set(z);
    }

    public DoubleProperty nodeXProperty() {
        return xProperty;
    }

    public DoubleProperty nodeYProperty() {
        return yProperty;
    }

    public DoubleProperty nodeZProperty() {
        return zProperty;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public double distance(GraphNode other) {
        return Math.sqrt(
                Math.pow(getNodeX() - other.getNodeX(), 2) +
                        Math.pow(getNodeY() - other.getNodeY(), 2) +
                        Math.pow(getNodeZ() - other.getNodeZ(), 2)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return Objects.equals(nodeId, graphNode.nodeId) &&
                getNodeX() == graphNode.getNodeX() &&
                getNodeY() == graphNode.getNodeY() &&
                getNodeZ() == graphNode.getNodeZ();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, getNodeX(), getNodeY(), getNodeZ());
    }
}