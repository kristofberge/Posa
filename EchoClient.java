import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class EchoClient {
	int socketNumber;
	Socket socket;
	PrintWriter send;
	BufferedReader receive, userInput;

	public static void main(String[] args) {
		int portnumber = Integer.parseInt(args[0]);
		EchoClient client = new EchoClient(portnumber);
		try {
			client.connectClient();
			client.init();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public EchoClient() {
		socketNumber = 80;
	}

	public EchoClient(int socketNumber) {
		this.socketNumber = socketNumber;

	}

	public void connectClient() throws UnknownHostException, IOException {
		socket = new Socket(InetAddress.getLocalHost(), socketNumber);
	}

	public void init() throws IOException {
		send = new PrintWriter(socket.getOutputStream(), true);
		receive = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		userInput = new BufferedReader(new InputStreamReader(System.in));

		String toServer = "Nothing to send to server";
		String fromServer = "Nothing received from server";

		System.out.println("Type something ('bye' to stop, 'shutdown' to shut down server)");

		while ((toServer = userInput.readLine()) != null) {
			send.println(toServer);
			fromServer = receive.readLine();
			System.out.println("Response from server: " + fromServer);
			if (toServer.trim().toLowerCase().equals("bye") || toServer.trim().toLowerCase().equals("shutdown"))
				break;
			System.out.println("Type something ('bye' to stop, 'shutdown' to shut down server)");
		}
		System.out.println("client shutting down");

		close();
	}

	public void close() throws IOException {
		send.close();
		receive.close();
		userInput.close();
		socket.close();

	}

}