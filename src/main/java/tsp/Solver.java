package tsp;

import java.util.*;

public class Solver implements Runnable {
    Buffer TSPBuffer;
    List<City> partition;   // this solver's subset of cities
    List<City> bestour;

    public Solver(Buffer TSPBuffer, List<City> partition) {
        this.TSPBuffer = TSPBuffer;
        this.partition = partition;
        this.bestour = new ArrayList<>();
    }

    @Override
    public void run() {
        long threadId = Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();

        int n = partition.size();
        if (n == 0) Thread.currentThread().interrupt();

        Set<City> visited = new HashSet<>();  // local to this solver

        // random start within this partition
        int startIndex = 0;

        City current = partition.get(startIndex);
        System.out.println("Thread " + threadId + " STARTED at " + startTime + " ms " + "at X:" + current.getX() + " Y at: " + current.getY() );
        visited.add(current);
        bestour.add(current);

        for (int step = 1; step < n; step++) {
            City next = null;
            double best = Double.POSITIVE_INFINITY;

            for (int j = 0; j < n; j++) {
                City tempCity = partition.get(j);
                if (visited.contains(tempCity)) continue;
                double d = current.distanceTo(tempCity);
                if (d < best) {
                    best = d;
                    next = tempCity;
                }
            }

            if (next == null) break;
            visited.add(next);
            bestour.add(next);
            current = next;
        }

        bestour.add(bestour.get(0)); // close partition loop
        TSPBuffer.summitTour(bestour); // submit to buffer
    }

    public static double length(List<City> cities, List<Integer> tour) {
        if (tour == null || tour.size() < 2) return 0.0;
        double total = 0.0;
        for (int i = 0; i < tour.size() - 1; i++) {
            City a = cities.get(tour.get(i));
            City b = cities.get(tour.get(i + 1));
            total += a.distanceTo(b);
        }
        return total;
    }
}
