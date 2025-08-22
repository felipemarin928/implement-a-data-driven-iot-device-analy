import java.util.*;
import java.io.*;
import java.net.*;

public class lr90_implement_a_dat {

    // Configuration file for IoT Device Analyzer
    private static final String DEVICE_LIST_FILE = "devices.config";
    private static final String SENSOR_DATA_FILE = "sensor_data.csv";
    private static final int ANALYSIS_INTERVAL = 30000; // 30 seconds

    // IoT Device Information
    private static final Map<String, Device> devices = new HashMap<>();

    static {
        loadDevicesFromFile(DEVICE_LIST_FILE);
    }

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new AnalyzerTask(), 0, ANALYSIS_INTERVAL, TimeUnit.MILLISECONDS);

        // Start the IoT device data collector
        new DeviceDataCollector().start();
    }

    private static void loadDevicesFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                devices.put(parts[0], new Device(parts[0], parts[1], Integer.parseInt(parts[2])));
            }
        } catch (IOException e) {
            System.err.println("Error loading devices from file: " + e.getMessage());
        }
    }

    private static class Device {
        private String id;
        private String type;
        private int port;

        public Device(String id, String type, int port) {
            this.id = id;
            this.type = type;
            this.port = port;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public int getPort() {
            return port;
        }
    }

    private static class AnalyzerTask implements Runnable {
        @Override
        public void run() {
            for (Device device : devices.values()) {
                try {
                    Socket socket = new Socket("localhost", device.getPort());
                    DataInputStream input = new DataInputStream(socket.getInputStream());

                    // Analyze sensor data from the device
                    String data = input.readUTF();
                    System.out.println("Received data from " + device.getId() + ": " + data);

                    // Process and store the data
                    storeSensorData(device, data);

                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error communicating with device " + device.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void storeSensorData(Device device, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SENSOR_DATA_FILE, true))) {
            writer.write(device.getId() + "," + data + "\n");
        } catch (IOException e) {
            System.err.println("Error storing sensor data to file: " + e.getMessage());
        }
    }

    private static class DeviceDataCollector extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    // Collect data from IoT devices
                    for (Device device : devices.values()) {
                        Socket socket = new Socket("localhost", device.getPort());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF("GET_DATA");
                        socket.close();
                    }
                    Thread.sleep(1000);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error collecting data from devices: " + e.getMessage());
                }
            }
        }
    }
}