package tsp;

import org.eclipse.paho.client.mqttv3.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DispatchSolver implements MqttCallback {
    private final List<City> cities;

    private final String BROKER = "tcp://test.mosquitto.org:1883";
    private final String available = "works/available";
    private final String assign = "works/assign";
    private final String resultTopic = "works/result";
    private boolean finish = false;
    private MqttClient client;
    private static final int LOCAL_WAIT = 1;
    private volatile Integer remoteCore = null;
    private volatile List<Integer> bestResult = null;


    public DispatchSolver(List<City> cities) throws MqttException {
        this.cities = new ArrayList<>(cities);

        String clientId = MqttClient.generateClientId();
        client = new MqttClient(BROKER, clientId);
        client.setCallback(this);
        client.connect();

        System.out.println("🔐 DispatchSolver: " + clientId + " connected to broker " + BROKER);

        client.subscribe(available, 2);
        client.subscribe(resultTopic, 2);
        //publish TSP to remote
        byte[] cityBytes = toBytes(cities);
        MqttMessage msg = new MqttMessage(cityBytes);
        msg.setQos(2);
        client.publish(assign, msg);
        finish = false;
    }
    public List<Integer> dispatchLocal(){
        int cpuCore = Runtime.getRuntime().availableProcessors();
        long seed = 100L;
        Random rand = new Random(seed);
        int startingIndex = rand.nextInt(cities.size());
        bestResult = NearestNeighborSolver.solve(cities, startingIndex);
        finish = true;
        return bestResult;
    }
    public List<Integer> dispatch(){

        try {
            byte[] citiesList = toBytes(cities);
            MqttMessage msg = new MqttMessage(citiesList);
            // publish cities to all remote worker
            client.publish(available, msg);
            System.out.println("Dispatch publishing msg to topic:  " + available);

            //RUN local Thread to COMPUTE
            Thread localThread = new Thread(() -> {
                try {
                    Thread.sleep(LOCAL_WAIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                List<Integer> localTour = dispatchLocal();
                if (!finish) {
                    bestResult = localTour;
                    finish = true;
                }
            });
            localThread.start();
            System.out.println("💻Local Solver running");
            //infinite loop to wait for result from remote and local
            // local will set finish == true and assign bestResult if local one compute
            // remote will set finish = true and assign bestReuslt if remote done compute
            while(!finish && bestResult == null) {

                System.out.println("...Waiting for result...");
                Thread.sleep(1000);
            }
            System.out.println("✅Dispatch Solved");

        } catch (MqttPersistenceException e) {
            throw new RuntimeException(e);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return bestResult;
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

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("❌ DispatchSolver connection lost: " +
                (cause == null ? "unknown" : cause.getMessage()));
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8).trim();

        if (topic.equals(resultTopic)) {
            System.out.println("📧 DispatchSolver Topic " + topic + " Message arrived: " );
            byte[] tourBytes = mqttMessage.getPayload();
            List<Integer> tour = (List<Integer>) fromBytes(tourBytes);
            if(!finish) {
                System.out.println("🙋‍♂️Remote Worker Finished before Local");
                bestResult = tour;

                finish = true;
            }
            else {
                System.out.println("💔Remote Worker Finished after Local");
            }

        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}