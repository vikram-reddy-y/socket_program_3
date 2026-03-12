import java.io.*;
import java.net.*;

public class client {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int PORT = 6031;

    public static void main(String[] args) {

        try {

            Socket socket = new Socket(SERVER_IP, PORT);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in));

            // Receiving thread
            Thread receiver = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {

                        if (msg.equals("KICKED")) {
                            System.out.println("Disconnected by server.");
                            System.exit(0);
                        }

                        if (msg.startsWith("CLIENT_LIST")) {

                            System.out.println("\nConnected Clients:");

                            String[] names =
                                    msg.substring(12).split(",");

                            for (String n : names) {
                                if (!n.isEmpty())
                                    System.out.println("- " + n);
                            }

                            System.out.println();
                        }
                        else {
                            System.out.println(msg);
                        }
                    }
                } catch (Exception e) {}
            });

            receiver.start();

            String input;

            while ((input = console.readLine()) != null) {

                out.println(input);

                if (input.equalsIgnoreCase("exit")) {
                    socket.close();
                    System.exit(0);
                }
            }

        } catch (Exception e) {
            System.out.println("Client error: " + e);
        }
    }
}