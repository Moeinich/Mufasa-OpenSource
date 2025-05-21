package helpers.Color;

import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DBSCAN {
    // A map to cache neighbors for points to avoid redundant computations
    private final ConcurrentHashMap<Point, List<Point>> neighborsCache = new ConcurrentHashMap<>();


    public List<Set<Point>> applyDBSCAN(List<Point> points, double eps, int minPts) {
        List<Set<Point>> clusters = new ArrayList<>();
        Set<Point> visited = new HashSet<>();

        for (Point point : points) {
            if (visited.contains(point)) continue;

            visited.add(point);
            List<Point> neighbors = getNeighbors(point, points, eps);

            if (neighbors.size() >= minPts) {
                Set<Point> cluster = new HashSet<>();
                clusters.add(expandCluster(point, cluster, visited, points, eps, minPts));
            }
        }

        return clusters;
    }

    private Set<Point> expandCluster(Point point, Set<Point> cluster, Set<Point> visited, List<Point> points, double eps, int minPts) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(point);

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();
            cluster.add(currentPoint);

            for (Point neighbor : getNeighbors(currentPoint, points, eps)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<Point> neighborPts = getNeighbors(neighbor, points, eps);

                    if (neighborPts.size() >= minPts) {
                        queue.add(neighbor);
                    }
                }

                cluster.add(neighbor);  // Always add to cluster, even if visited
            }
        }

        return cluster;
    }

    /**
     * Gets the neighboring points of a given point based on the eps distance.
     *
     * @param point  The point for which neighbors are to be found.
     * @param points List of all points.
     * @return List of neighboring points.
     */
    private List<Point> getNeighbors(Point point, List<Point> points, double eps) {
        // Return cached neighbors if available
        if (neighborsCache.containsKey(point)) {
            return neighborsCache.get(point);
        }

        List<Point> neighbors = new ArrayList<>();
        for (Point candidate : points) {
            if (point.distance(candidate) < eps) {
                neighbors.add(candidate);
            }
        }

        // Cache the computed neighbors
        neighborsCache.put(point, neighbors);
        return neighbors;
    }
}