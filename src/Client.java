import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // Wrap the byte stream in a character stream
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));   // Same thing ^
            this.username = username;
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage() {
        try {
            // This gives the ClientHandler this client's username.
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush(); // We flush here because buffered data is not sent unless the buffer is full.

           // This sends the message.
           Scanner scanner = new Scanner(System.in);
           while (socket.isConnected()) {
               String messageToSend = scanner.nextLine();
               bufferedWriter.write(username + ": " + messageToSend);
               bufferedWriter.newLine();
               bufferedWriter.flush(); // We flush here because buffered data is not sent unless the buffer is full.
           }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage() {
        /*
         We use multithreading here so that the user is not stuck waiting for a message or for a user to join, so that
         they can actually send their own messages.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            /*
             We don't have to close OutputStreamWriter or InputStreamWriter because underlying streams are closed when
             outer streams are closed. Closing sockets also closes its IO streams. This is also why we need to catch
             IO exceptions.
             */
            if (bufferedReader != null) {
                bufferedReader.close();
            }

            if (bufferedWriter != null) {
                bufferedWriter.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username: ");
        String username = scanner.nextLine();
        Socket socket = new Socket("localhost", 1234);
        Client client = new Client(socket, username);
        client.listenForMessage();
        client.sendMessage();
    }
}
