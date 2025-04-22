package helpers.grapher;

import helpers.grapher.utils.GraphEdge;
import helpers.grapher.utils.GraphNode;
import helpers.grapher.utils.dataclasses.Quadtree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Graph {
    private final ConcurrentMap<String, GraphNode> nodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<GraphNode, Set<GraphEdge>> adjacencyList = new ConcurrentHashMap<>();

    private final Quadtree quadtree;

    public Graph() {
        this.quadtree = new Quadtree(0, 0, 0, 12800, 46568);
    }

    public void addNode(GraphNode node) {
        // Avoid re-adding the same node
        if (nodes.containsKey(node.getNodeId())) {
            return;
        }

        // Add to nodes map
        nodes.put(node.getNodeId(), node);

        // Ensure adjacency list entry exists with an empty Set
        adjacencyList.putIfAbsent(node, ConcurrentHashMap.newKeySet());

        // Insert into quadtree
        quadtree.insert(node);
    }

    public void removeNode(GraphNode node) {
        // Remove all edges associated with the node
        Set<GraphEdge> edgesToRemove = adjacencyList.remove(node);
        if (edgesToRemove != null) {
            for (GraphEdge edge : edgesToRemove) {
                GraphNode otherNode = edge.getStartNode().equals(node) ? edge.getEndNode() : edge.getStartNode();
                adjacencyList.get(otherNode).remove(edge);
            }
        }

        // Remove the node from the nodes map and quadtree
        nodes.remove(node.getNodeId());
        quadtree.remove(node);
    }

    public void addEdge(GraphEdge edge) {
        GraphNode start = edge.getStartNode();
        GraphNode end = edge.getEndNode();

        if (!nodes.containsKey(start.getNodeId()) || !nodes.containsKey(end.getNodeId())) {
            throw new IllegalArgumentException("Both nodes must be part of the graph.");
        }

        if (!adjacencyList.get(start).contains(edge)) {
            adjacencyList.get(start).add(edge);
        }
        if (!adjacencyList.get(end).contains(edge)) {
            adjacencyList.get(end).add(edge);
        }
    }

    public void removeEdge(GraphEdge edge) {
        adjacencyList.get(edge.getStartNode()).remove(edge);
        adjacencyList.get(edge.getEndNode()).remove(edge);
    }

    public Collection<GraphNode> getNodes() {
        return nodes.values();
    }

    public List<GraphEdge> getEdges() {
        return adjacencyList.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public String getNextAvailableNodeId() {
        Set<Integer> existingIds = ConcurrentHashMap.newKeySet();
        for (String nodeId : nodes.keySet()) {
            if (nodeId.startsWith("Node")) {
                try {
                    int id = Integer.parseInt(nodeId.substring(4));
                    existingIds.add(id);
                } catch (NumberFormatException e) {
                    // Ignore any node IDs that don't match the expected format
                }
            }
        }
        int newId = 1;
        while (existingIds.contains(newId)) {
            newId++;
        }
        return "Node" + newId;
    }

    public List<GraphNode> retrieveNodesInArea(int xMin, int xMax, int yMin, int yMax) {
        return quadtree.retrieveInArea(xMin, xMax, yMin, yMax)
                .stream()
                .filter(node -> adjacencyList.containsKey(node)) // Ensure adjacencyList consistency
                .collect(Collectors.toList());
    }

    public GraphNode[] findShortestPath(GraphNode startNode, GraphNode endNode) {
        // Ensure the end node exists in the graph
        if (!nodes.containsKey(endNode.getNodeId())) {
            throw new IllegalArgumentException("End node not in graph.");
        }

        // Find the best possible start node
        GraphNode bestStartNode = findBestStartNode(startNode);

        // Initialize data structures for Dijkstra's algorithm
        Map<GraphNode, Double> distances = new HashMap<>();
        Map<GraphNode, GraphNode> previousNodes = new HashMap<>();
        PriorityQueue<GraphNodeDistance> priorityQueue = new PriorityQueue<>();

        // Initialize distances
        for (GraphNode node : nodes.values()) {
            if (node.equals(bestStartNode)) {
                distances.put(node, 0.0);
                priorityQueue.add(new GraphNodeDistance(node, 0.0));
            } else {
                distances.put(node, Double.POSITIVE_INFINITY);
            }
        }

        // Run Dijkstra's algorithm
        while (!priorityQueue.isEmpty()) {
            GraphNodeDistance current = priorityQueue.poll();
            GraphNode currentNode = current.node;

            if (currentNode.equals(endNode)) {
                break; // Found the shortest path to endNode
            }

            for (GraphEdge edge : adjacencyList.get(currentNode)) {
                GraphNode neighbor = edge.getStartNode().equals(currentNode) ? edge.getEndNode() : edge.getStartNode();
                double newDist = distances.get(currentNode) + currentNode.distance(neighbor);

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    previousNodes.put(neighbor, currentNode);
                    priorityQueue.add(new GraphNodeDistance(neighbor, newDist));
                }
            }
        }

        // Reconstruct the path from endNode to startNode
        List<GraphNode> path = new ArrayList<>();
        GraphNode currentNode = endNode;
        while (currentNode != null) {
            path.add(currentNode);
            currentNode = previousNodes.get(currentNode);
        }

        // Reverse the path to start from startNode
        Collections.reverse(path);

        // Check if the start node is at the beginning of the path
        if (!path.get(0).equals(bestStartNode)) {
            System.out.println("No path found from startNode to endNode: " + "start node: " + startNode.getNodeX() + "," + startNode.getNodeY() + "," + startNode.getNodeZ() + " -- end node: " + endNode.getNodeX() + "," + endNode.getNodeY() + "," + endNode.getNodeZ());
            return new GraphNode[0]; // Return an empty array if no path exists
        }

        return path.toArray(new GraphNode[0]);
    }

    public GraphNode findBestStartNode(GraphNode currentLocation) {
        // Get all nearby nodes from the quadtree
        int searchRadius = 50;
        List<GraphNode> nearbyNodes = quadtree.retrieveInArea(
                currentLocation.getNodeX() - searchRadius,
                currentLocation.getNodeX() + searchRadius,
                currentLocation.getNodeY() - searchRadius,
                currentLocation.getNodeY() + searchRadius
        );

        // Sort nodes by distance to the current location
        nearbyNodes.sort(Comparator.comparingDouble(n -> n.distance(currentLocation)));

        // Check if the closest node has edges, otherwise fallback to the next best node
        for (GraphNode node : nearbyNodes) {
            if (!adjacencyList.get(node).isEmpty()) {
                return node; // Return the first node with edges
            }
        }

        // If none of the nearby nodes have edges, return the closest node anyway
        return nearbyNodes.get(0);
    }


    /**
     * Helper class to associate GraphNode with its current distance in the priority queue.
     */
    private static class GraphNodeDistance implements Comparable<GraphNodeDistance> {
        GraphNode node;
        double distance;

        public GraphNodeDistance(GraphNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(GraphNodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}
