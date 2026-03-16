package tsp.remote;

import org.eclipse.paho.client.mqttv3.*;

import java.rmi.Remote;
import java.util.Vector;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main implements MqttCallback {
    private static String avalable = "works/availible";
    private static MqttClient client = null;

    public static void main(String[] args) {

        //publish available core to a dispatcher
        Thread t = new Thread(new RemoteSolver());
        t.start();
    }



    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("❌Remote Main Connection to broker lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {


    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            System.out.println("📥 Remote Main Delivery complete for: " + token.getMessageId());
        } catch (Exception e) {
            System.out.println("📥 Remote Main Delivery complete, but failed to get message ID.");
        }
    }
}