package helpers.grapher;

import helpers.grapher.utils.GraphEdge;
import helpers.grapher.utils.GraphNode;
import helpers.grapher.utils.dataclasses.GraphData;
import helpers.grapher.utils.dataclasses.GraphEdgeData;
import helpers.grapher.utils.dataclasses.GraphNodeData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphHelpers {
    public static GraphNode findNearestNode(Graph graph, int x, int y) {
        int searchRadius = 50;
        GraphNode nearestNode = null;
        double minDistance = Double.MAX_VALUE;

        // Gradually increase search radius until a node is found
        while (nearestNode == null) {
            // Retrieve nodes within a square area around the point (x, y)
            List<GraphNode> nearbyNodes = graph.retrieveNodesInArea(x - searchRadius, x + searchRadius, y - searchRadius, y + searchRadius);

            // Find the nearest node among the retrieved nodes
            for (GraphNode node : nearbyNodes) {
                double distance = Math.hypot(node.getNodeX() - x, node.getNodeY() - y);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestNode = node;
                }
            }

            // If no nodes are found, increase the search radius
            if (nearestNode == null) {
                searchRadius *= 2;  // Double the search radius
            }
        }
        return nearestNode;
    }

    public static Graph convertToGraph(GraphData graphData) {
        Graph graph = new Graph();
        Map<String, GraphNode> nodeMap = new HashMap<>();

        for (GraphNodeData nodeData : graphData.getNodes()) {
            GraphNode node = new GraphNode(nodeData.getNodeId(), nodeData.getX(), nodeData.getY(), nodeData.getZ());
            graph.addNode(node);
            nodeMap.put(node.getNodeId(), node);
        }

        for (GraphEdgeData edgeData : graphData.getEdges()) {
            GraphNode startNode = nodeMap.get(edgeData.getStartNodeId());
            GraphNode endNode = nodeMap.get(edgeData.getEndNodeId());

            if (startNode != null && endNode != null) {
                GraphEdge edge = new GraphEdge(startNode, endNode);
                graph.addEdge(edge);
            }
        }

        return graph;
    }

    public static GraphData convertToGraphData(Graph graph) {
        GraphData graphData = new GraphData();
        for (GraphNode node : graph.getNodes()) {
            graphData.getNodes().add(new GraphNodeData(node.getNodeId(), node.getNodeX(), node.getNodeY(), node.getNodeZ()));
        }
        for (GraphEdge edge : graph.getEdges()) {
            graphData.getEdges().add(new GraphEdgeData(edge.getStartNode().getNodeId(), edge.getEndNode().getNodeId()));
        }
        return graphData;
    }
}
