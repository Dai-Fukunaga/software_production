
package server;

import java.io.*;
import java.net.*;

class ServerThread extends ServerFunc {
    Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            // db.insertData("usr", "./music", sdf.format(date).toString(), "");

            String line;
            do {
                // メッセージを受け取るフェーズ
                line = dis.readUTF();
                switch (line) {
                    case "send":
                        receiveFile(dis, dos);
                        break;
                    case "get":
                        sendFile(dis, dos);
                        break;
                    case "end":
                        System.out.println("end");
                        break;
                }
            } while (!line.equals("end"));

        } catch (NumberFormatException | NullPointerException e) { // clientが接続を切った場合
            System.out.println(Thread.currentThread().getName() + "が切断されました");
        } catch (SocketException e) { // java.net.SocketException: Connection reset
            System.out.println(Thread.currentThread().getName() + "が切断されました");
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(e);
            } finally {
                System.out.println("closing...");
            }
        }
    }
}

public class Server {
    static int MAIN_PORT = 8080;
    static int NOTIFY_PORT = 8081;

    public static void main(String args[]) throws IOException {
        ServerSocket mainsoc = new ServerSocket(MAIN_PORT);
        ServerSocket notifysoc = new ServerSocket(NOTIFY_PORT);

        Thread notifyServerThread = new Thread(() -> {
            try {
                while (true) {
                    Socket clientSocket = notifysoc.accept();
                    ServerFunc.addClient(clientSocket);
                    new Thread(() -> {
                        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                            while (true) {
                                String message = dis.readUTF();
                                if (message.equals("notify")) {
                                    ServerFunc.notifyClients();
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Error in notify server thread: " + e.getMessage());
                        }
                    }).start();
                }
            } catch (IOException e) {
                System.out.println("Error in notify server: " + e.getMessage());
            }
        });

        notifyServerThread.start();

        try {
            while (true) {
                Socket s = mainsoc.accept();
                ServerThread st = new ServerThread(s);
                st.start();
            }
        } catch (IOException e) {
            System.out.println("Error in main server: " + e.getMessage());
        } finally {
            try {
                mainsoc.close();
            } catch (IOException e) {
                System.out.println("Error closing main server socket: " + e.getMessage());
            }
            try {
                notifysoc.close();
            } catch (IOException e) {
                System.out.println("Error closing notify server socket: " + e.getMessage());
            }
        }
    }
}
