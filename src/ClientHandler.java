import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    /*
     This array list stores clients so that when one client sends a message, we are able to loop through the clients
     to broadcast the message.
     */
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader; // Used to read data
    private BufferedWriter bufferedWriter; // Used to write data to clients
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;

            // Stream to send messages
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // Wrap the byte stream in a character stream

            // Stream to read data (messages)
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));   // Same thing ^

            // Set client username
            this.clientUsername = bufferedReader.readLine();

            // Add client to arraylist
            clientHandlers.add(this); // this specific line is what made me finally understand the "this" keyword
            broadcastMessage("SERVER: " + clientUsername + " has joined the chatroom!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /*
     "When an object implementing interface Runnable is used to create a thread, starting the thread causes the object's run
     method to be called in that separately executing thread." - Oracle Docs
     I put this here because I had no idea what Runnable did.
     */
    @Override
    public void run() {
        /*
         We use multithreading here so that the user is not stuck waiting for a message or for a user to join, so that
         they can actually send their own messages.
         */
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine(); // readLine is a blocking method, which, again, is why we are using multithreading.
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break; // Breaks out of the while loop when the client disconnects
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler: clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush(); // We flush here because buffered data is not sent unless the buffer is full.
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + this.clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
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
}
