package tsp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Buffer {
    private final List<List<City>> clusterTours;
    ReentrantLock lock = new ReentrantLock();
    public Buffer() {
        this.clusterTours = new ArrayList<>();
        this.lock = new ReentrantLock();
    }
    public void summitTour(List<City> tour) {
        lock.lock();
        try {
            clusterTours.add(new ArrayList<>(tour));   // keep each cluster separate
        } finally {
            lock.unlock();
        }
    }

    public List<List<City>> getClusterTours() {
        lock.lock();
        try {
            List<List<City>> copy = new ArrayList<>();
            for (List<City> t : clusterTours) {
                copy.add(new ArrayList<>(t));
            }
            return copy;
        } finally {
            lock.unlock();
        }
    }
}