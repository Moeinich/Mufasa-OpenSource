package helpers.grapher.utils.dataclasses;

import helpers.grapher.utils.GraphNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Quadtree {
    private static final int MAX_POINTS = 4;  // Maximum points per quadrant before splitting
    private static final int MAX_LEVELS = 5;  // Maximum depth of the quadtree

    private final int level;
    private final List<GraphNode> nodes;
    private final Quadtree[] subtrees;
    private int x, y, width, height;

    // Constructor to initialize a Quadtree node
    public Quadtree(int level, int x, int y, int width, int height) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.nodes = new ArrayList<>();
        this.subtrees = new Quadtree[4];
    }

    // Expand the quadtree to accommodate a node that is outside the current bounds
    public void expandAndInsert(GraphNode node) {
        // Check if the node fits within the current quadtree bounds
        while (!isInBounds(node)) {
            // Determine new boundaries by expanding the current area
            int newWidth = width * 2;
            int newHeight = height * 2;
            int newX = x;
            int newY = y;

            // Create a new larger root Quadtree
            Quadtree newRoot = new Quadtree(level - 1, newX, newY, newWidth, newHeight);

            // Find which quadrant the current quadtree should fall into in the new root
            int quadrant = getIndexBasedOnNewBounds(newX, newY, newWidth, newHeight);

            // Insert the old quadtree into the correct quadrant of the new root
            newRoot.subtrees[quadrant] = this;

            // Update the current quadtree to be the new root
            this.x = newX;
            this.y = newY;
            this.width = newWidth;
            this.height = newHeight;
        }

        // Now insert the node into the expanded quadtree
        insert(node);
    }

    // Check if a node is within the current bounds
    private boolean isInBounds(GraphNode node) {
        return node.getNodeX() >= x && node.getNodeX() < x + width
                && node.getNodeY() >= y && node.getNodeY() < y + height;
    }

    // Determine which quadrant the old quadtree fits into after expansion
    private int getIndexBasedOnNewBounds(int newX, int newY, int newWidth, int newHeight) {
        boolean left = (x < newX + (newWidth / 2));
        boolean top = (y < newY + (newHeight / 2));
        if (left && top) {
            return 0;  // Top-left quadrant
        } else if (!left && top) {
            return 1;  // Top-right quadrant
        } else if (left && !top) {
            return 2;  // Bottom-left quadrant
        } else {
            return 3;  // Bottom-right quadrant
        }
    }

    // Clear the quadtree
    public void clear() {
        nodes.clear();
        for (int i = 0; i < subtrees.length; i++) {
            if (subtrees[i] != null) {
                subtrees[i].clear();
                subtrees[i] = null;
            }
        }
    }

    // Remove a node from the quadtree
    public synchronized boolean remove(GraphNode node) {
        if (subtrees[0] != null) {
            int index = getIndex(node);
            if (index != -1) {
                boolean removed = subtrees[index].remove(node);
                if (removed && subtrees[index].isEmpty()) {
                    collapse();  // Collapse the quadtree if a quadrant becomes empty
                }
                return removed;
            }
        }

        // If the node is in this level's list of nodes, remove it

        return nodes.remove(node);
    }

    // Check if the quadtree is empty
    public synchronized boolean isEmpty() {
        if (!nodes.isEmpty()) {
            return false;
        }
        for (Quadtree subtree : subtrees) {
            if (subtree != null && !subtree.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Collapse the quadtree by removing empty subtrees
    private void collapse() {
        if (subtrees[0] != null) {
            boolean allEmpty = true;
            for (Quadtree subtree : subtrees) {
                if (subtree != null && !subtree.isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                Arrays.fill(subtrees, null);
            }
        }
    }

    // Split the quadtree into 4 sub-quadrants
    private void split() {
        int subWidth = width / 2;
        int subHeight = height / 2;
        int xMid = x + subWidth;
        int yMid = y + subHeight;

        subtrees[0] = new Quadtree(level + 1, x, y, subWidth, subHeight);
        subtrees[1] = new Quadtree(level + 1, xMid, y, subWidth, subHeight);
        subtrees[2] = new Quadtree(level + 1, x, yMid, subWidth, subHeight);
        subtrees[3] = new Quadtree(level + 1, xMid, yMid, subWidth, subHeight);
    }

    // Determine which quadrant a node belongs to
    private int getIndex(GraphNode node) {
        int index = -1;
        double verticalMidpoint = x + (width / 2.0);
        double horizontalMidpoint = y + (height / 2.0);

        boolean topQuadrant = node.getNodeY() < horizontalMidpoint;
        boolean bottomQuadrant = node.getNodeY() >= horizontalMidpoint;

        if (node.getNodeX() < verticalMidpoint && node.getNodeX() >= x) {
            if (topQuadrant) {
                index = 0;  // Top-left quadrant
            } else if (bottomQuadrant) {
                index = 2;  // Bottom-left quadrant
            }
        } else if (node.getNodeX() >= verticalMidpoint) {
            if (topQuadrant) {
                index = 1;  // Top-right quadrant
            } else if (bottomQuadrant) {
                index = 3;  // Bottom-right quadrant
            }
        }
        return index;
    }

    // Insert a node into the quadtree
    public synchronized void insert(GraphNode node) {
        // If the node doesn't fit within the current bounds, expand the quadtree
        if (!isInBounds(node)) {
            expandAndInsert(node);
            return;
        }

        if (subtrees[0] != null) {
            int index = getIndex(node);
            if (index != -1) {
                subtrees[index].insert(node);
                return;
            }
        }

        nodes.add(node);

        if (nodes.size() > MAX_POINTS && level < MAX_LEVELS) {
            if (subtrees[0] == null) {
                split();  // Split the node into 4 sub-quadrants
            }

            int i = 0;
            while (i < nodes.size()) {
                int index = getIndex(nodes.get(i));
                if (index != -1) {
                    subtrees[index].insert(nodes.remove(i));
                } else {
                    i++;
                }
            }
        }
    }

    // Retrieve all nodes that could collide with a given node
    public synchronized List<GraphNode> retrieve(List<GraphNode> returnNodes, GraphNode node) {
        int index = getIndex(node);
        if (index != -1 && subtrees[0] != null) {
            subtrees[index].retrieve(returnNodes, node);
        }

        returnNodes.addAll(nodes);

        return returnNodes;
    }

    // Retrieve all nodes within a specific area
    public synchronized List<GraphNode> retrieveInArea(int xMin, int xMax, int yMin, int yMax) {
        List<GraphNode> result = new ArrayList<>();

        if (!intersects(xMin, xMax, yMin, yMax)) {
            return result; // No intersection with this quadrant
        }

        for (GraphNode node : nodes) {
            if (node.getNodeX() >= xMin && node.getNodeX() <= xMax && node.getNodeY() >= yMin && node.getNodeY() <= yMax) {
                result.add(node);
            }
        }

        if (subtrees[0] != null) {
            for (Quadtree subtree : subtrees) {
                result.addAll(subtree.retrieveInArea(xMin, xMax, yMin, yMax));
            }
        }

        return result;
    }

    // Check if the given area intersects with this quadtree's region
    private boolean intersects(int xMin, int xMax, int yMin, int yMax) {
        return !(xMin > x + width || xMax < x || yMin > y + height || yMax < y);
    }
}
