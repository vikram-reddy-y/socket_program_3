import java.io.*;
import java.net.*;
import java.util.*;

public class server {

    private static final int PORT = 6031;

    // Directory: clientName -> ClientHandler
    private static Map<String, ClientHandler> clients =
            Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Server started at IP: "
                + InetAddress.getLocalHost().getHostAddress()
                + " PORT: " + PORT);

        // Server console thread
        new Thread(() -> {
            try {
                BufferedReader console =
                        new BufferedReader(new InputStreamReader(System.in));
                String cmd;

                while ((cmd = console.readLine()) != null) {

                    if (cmd.equalsIgnoreCase("list")) {
                        showClients();
                    }

                    else if (cmd.startsWith("kick ")) {
                        String name = cmd.substring(5);
                        kickClient(name);
                    }

                    else if (cmd.startsWith("send ")) {
                        String[] parts = cmd.split(" ", 3);
                        if (parts.length == 3) {
                            sendToClient(parts[1], parts[2]);
                        }
                    }

                    else if (cmd.startsWith("broadcast ")) {
                        broadcast("SERVER: " + cmd.substring(10));
                    }
                }
            } catch (Exception e) {}
        }).start();

        // Accept multiple clients
        while (true) {
            Socket socket = serverSocket.accept();
            new ClientHandler(socket).start();
        }
    }

    //  Show client directory
    public static void showClients() {
        System.out.println("\nConnected Clients:");
        synchronized (clients) {
            for (String name : clients.keySet()) {
                ClientHandler ch = clients.get(name);
                System.out.println("Name: " + name +
                        " | IP: " + ch.socket.getInetAddress());
            }
        }
        System.out.println();
    }

    // Send updated client list to all
    public static void sendUpdatedList() {
        StringBuilder list = new StringBuilder("CLIENT_LIST ");
        synchronized (clients) {
            for (String name : clients.keySet()) {
                list.append(name).append(",");
            }
        }
        broadcast(list.toString());
    }

    // Broadcast message
    public static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler ch : clients.values()) {
                ch.out.println(message);
            }
        }
    }

    // Send message to individual client
    public static void sendToClient(String name, String message) {
        ClientHandler ch = clients.get(name);
        if (ch != null) {
            ch.out.println("SERVER (Private): " + message);
        } else {
            System.out.println("Client not found.");
        }
    }

    // Kick client
    public static void kickClient(String name) {
        ClientHandler ch = clients.get(name);
        if (ch != null) {
            ch.out.println("You have been removed by server.");
            ch.closeConnection();
            clients.remove(name);
            sendUpdatedList();
            System.out.println(name + " has been kicked.");
        }
    }

    // Client Thread
    static class ClientHandler extends Thread {

        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {

            try {
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                out = new PrintWriter(
                        socket.getOutputStream(), true);

                // Ask unique name
                while (true) {
                    out.println("Enter your name:");
                    clientName = in.readLine();

                    if (clientName == null) return;

                    synchronized (clients) {
                        if (!clients.containsKey(clientName)) {
                            clients.put(clientName, this);
                            break;
                        }
                    }
                    out.println("Name already exists. Try again.");
                }

                System.out.println(clientName + " connected from "
                        + socket.getInetAddress());

                sendUpdatedList();

                String message;

                while ((message = in.readLine()) != null) {

                    // Always display message on server
                    System.out.println(clientName + ": " + message);

                    // Private message
                    if (message.startsWith("msg ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            String target = parts[1];
                            String msg = parts[2];
                            ClientHandler receiver = clients.get(target);
                            if (receiver != null) {
                                receiver.out.println("(Private) "
                                        + clientName + ": " + msg);
                            }
                        }
                    }

                    // Broadcast
                    else if (message.startsWith("broadcast ")) {
                        broadcast(clientName + ": "
                                + message.substring(10));
                    }

                    // Request list
                    else if (message.equalsIgnoreCase("list")) {
                        sendUpdatedList();
                    }

                    // Exit
                    else if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                }

            } catch (Exception e) {
                System.out.println("Error: " + e);
            } finally {
                try {
                    clients.remove(clientName);
                    sendUpdatedList();
                    socket.close();
                    System.out.println(clientName + " disconnected.");
                } catch (Exception e) {}
            }
        }

        void closeConnection() {
            try { socket.close(); } catch (Exception e) {}
        }
    }
}