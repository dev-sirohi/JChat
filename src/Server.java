import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    // A serversocket to listen to incoming connections
    private ServerSocket serverSocket;

    // A constructor to set up our serversocket.
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    // A startserver method to keep the server running using while loop and checking whether the serversocket is running or closed.
    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {

                // accept() returns a socket object
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected!");

                // When a class implements a runnable interface, the instances of the class are run on a separate thread.
                // Specifically, whatever is in the run method of the class that implements runnable.
                // This will take the socket info and use it on the client side.
                ClientHandler clientHandler = new ClientHandler(socket);

                // However, to create a new thread, we need to create a new thread object that (must) takes the instance of the class
                // -that implements the runnable class as it's constructor parameter.
                // Then we just use the start method to initiate the thread.
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closedServerSocket();
        }
    }

    // Creating an exception method in case an error occurs so we can just shut down our server sockets.
    public void closedServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Creates a server socket bound to a port.
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}

class ClientHandler implements Runnable {
    // To keep track of all our clients
    // This is static because we want it to belong to the class, not to each object of the class
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    // A socket object that will be passed from our server class.
    // Used to make the connection between the client and the server.
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            // Now, a socket is a connection between clienthandler and client.
            // Each socket consists of an inputstream and outputstream.
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // In java, there are two types of streams. Byte stream and Character stream.
            // The latter ends with writer while the former ends with just stream.
            // Guess what the above code line does.
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // readline reads a LINE as in up until a newline character.
            this.clientUsername = bufferedReader.readLine();
            // We'll add THIS because the array accepts ClientHandler objects, not String objects.
            clientHandlers.add(this);
            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                // This next line is a blocking operation.
                // We need a new thread specifically because of this so that the entire app doesn't stop.
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                // Break keyword is important here because otherwise it's going to keep executing the previous code line.
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    // Each clinthandler object has a bufferedwriter
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    // We need to manually FLUSH the buffer
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        // If we call the close method on null, we get a nullpointerexcption.
        try {

            // When we close these things, everything inside the wrapper class gets closed as well.
            // We don't need to close the inputstreams in any of the below. It happens automatically.

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


// We don't have a main here because this is being run by the thread.start in the server class.