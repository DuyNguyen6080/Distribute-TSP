package tsp.remote;


import org.eclipse.paho.client.mqttv3.*;
import tsp.City;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple implementation of the Nearest Neighbor heuristic for TSP.
 *
 * @author javiergs
 * @version 2.0
 */
public class RemoteSolver implements Runnable, MqttCallback {
    private final String broker = "tcp://test.mosquitto.org:1883";
    private final String topic_available = "works/available";
    private final String topic_assign = "works/assign";
    private final String topic_result = "works/result";
    private String Id;
    private MqttClient client = null;
    public static List<Integer> solve(List<City> cities, int startIndex) {
        int n = cities.size();
        if (n == 0) return List.of();
        if (startIndex < 0 || startIndex >= n) startIndex = 0;
        boolean[] used = new boolean[n];
        List<Integer> tour = new ArrayList<>(n + 1);
        int current = startIndex;
        used[current] = true;
        tour.add(current);
        for (int step = 1; step < n; step++) {
            int next = -1;
            double best = Double.POSITIVE_INFINITY;
            City curCity = cities.get(current);
            for (int j = 0; j < n; j++) {
                if (used[j]) continue;
                double d = curCity.distanceTo(cities.get(j));
                if (d < best) {
                    best = d;
                    next = j;
                }
            }
            used[next] = true;
            tour.add(next);
            current = next;
        }
        tour.add(tour.get(0));
        return tour;
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

    @Override
    public void run() {
        try {
            this.Id= MqttClient.generateClientId();
            client = new MqttClient(broker, this.Id);
            client.setCallback(this);
            client.connect();
            client.subscribe(topic_available);
            System.out.println("Remoteworker: " + this.Id + " ↗️ Connected to broker: " + broker);
            while(true) {
                Thread.sleep(1000);
            }

        } catch (MqttSecurityException e) {
            throw new RuntimeException(e);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] toBytes(Object obj) {
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)
        ) {
            out.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println("RemoteSolver ERROR converting toBtyte" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Object fromBytes(byte[] bytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis);
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("RemoteSolver ERROR converting fromBytes" + e.getMessage());
        }
        return null;
    }
    private void sendback (String topic, List<Integer> tour) {
        byte[] tourBytes = toBytes(tour);
        MqttMessage message = new MqttMessage(tourBytes);
        try {
            client.publish(topic, message);
        } catch (MqttPersistenceException e) {
            throw new RuntimeException(e);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage){
        if(topic.equals(topic_available)) {
            System.out.println("RemoteSolver recived payload");
            byte[] payloadBytes = mqttMessage.getPayload();
            List<City> temp = (List<City>) fromBytes(payloadBytes);
            if (temp == null) return;
            Random rand = new Random(100L);
            int startingIndex = rand.nextInt(temp.size());
            List<Integer> result = solve(temp, startingIndex);

            sendback(topic_result, result );
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
