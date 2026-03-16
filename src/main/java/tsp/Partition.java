package tsp;

import java.util.ArrayList;
import java.util.List;

public class Partition {

    public static List<List<City>> partition(List<City> cities, int N) {
        if (cities == null || cities.isEmpty() || N <= 0) return new ArrayList<>();
        if (N >= cities.size()) {
            List<List<City>> result = new ArrayList<>();
            for (City c : cities) {
                List<City> single = new ArrayList<>();
                single.add(c);
                result.add(single);
            }
            return result;
        }

        // KMeans++ init — pick N spread out cities as starting centroids
        List<double[]> centroids = new ArrayList<>();
        centroids.add(new double[]{cities.get(0).getX(), cities.get(0).getY()});

        for (int i = 1; i < N; i++) {
            City farthest = null;
            double maxDist = -1;

            for (City c : cities) {
                // find distance from c to its NEAREST centroid
                double minDist = Double.MAX_VALUE;
                for (double[] centroid : centroids) {           // NO STREAM
                    double d = distance(centroid, c);
                    if (d < minDist) {
                        minDist = d;
                    }
                }
                // track city whose nearest centroid is farthest
                if (minDist > maxDist) {
                    maxDist = minDist;
                    farthest = c;
                }
            }
            centroids.add(new double[]{farthest.getX(), farthest.getY()});
        }

        // init partitions
        List<List<City>> partitions = new ArrayList<>();
        for (int i = 0; i < N; i++) partitions.add(new ArrayList<>());

        // iterate until stable
        for (int iter = 0; iter < 100; iter++) {
            // clear partitions
            for (List<City> p : partitions) p.clear();

            // assign each city to nearest centroid
            for (City c : cities) {
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < N; i++) {
                    double d = distance(centroids.get(i), c);
                    if (d < minDist) {
                        minDist = d;
                        nearest = i;
                    }
                }
                partitions.get(nearest).add(c);
            }

            // recompute centroids as average x and y of each cluster
            boolean changed = false;
            for (int i = 0; i < N; i++) {
                if (partitions.get(i).isEmpty()) continue;
                double sumX = 0, sumY = 0;
                for (City c : partitions.get(i)) {      // NO STREAM
                    sumX += c.getX();
                    sumY += c.getY();
                }
                double avgX = sumX / partitions.get(i).size();
                double avgY = sumY / partitions.get(i).size();
                double[] newCentroid = new double[]{avgX, avgY};
                if (distance(newCentroid, avgX, avgY) > 1e-6) changed = true;
                centroids.set(i, newCentroid);
            }

            if (!changed) break;
        }

        return partitions;
    }
    private static double distance(double[] centroid, City c) {
        double dx = centroid[0] - c.getX();
        double dy = centroid[1] - c.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double distance(double[] centroid, double x, double y) {
        double dx = centroid[0] - x;
        double dy = centroid[1] - y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
