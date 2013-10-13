import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoEcho {

	public static void main(String[] args) {

		// Anonymous inner class that inherits from TrafficHandler and returns
		// exactly what the client sent.
		TrafficHandler echoHandler = new TrafficHandler() {
			@Override
			public MyTrafficEvent handleEvent(MyTrafficEvent e) {
				return e;
			}
		};

		EchoReactor reactor;
		try {
			reactor = new EchoReactor(666);
			reactor.registerTrafficHandler(echoHandler);
			reactor.registerConnectionHandler(new AcceptHandler());
			reactor.handleEvents();
		} catch (IOException e1) {
			e1.printStackTrace();
		} // the server will listen on port 666
		
	}

	// General interface for all EventHandlers
	private static abstract class EventHandler {

		protected EchoReactor reactor;

		public EventHandler getHandle() {
			return this;
		}

		public void setReactor(EchoReactor reactor) {
			this.reactor = reactor;
		}
	}

	// MyAbstractEvents are encapsulating objects used to pass data between
	// Reactor and Handlers.
	private static interface MyAbstractEvent {
	}

	// The parent class for classes that will handle the incoming traffic from
	// the client
	private static abstract class TrafficHandler extends EventHandler {

		
		// The method that will handle the incoming server messages. It takes a
		// MyTrafficEvent as input with the incoming message from the client and
		// returns a MyTrafficEvent which will have the server response.
		public abstract MyTrafficEvent handleEvent(MyTrafficEvent e);
		
	}

	// The parent class for classes that will set up and close network
	// connections.
	static abstract class ConnectionHandler extends EventHandler {
		public abstract void handleEvent(ServerSocket serverSocket);

		public abstract void close();
		
	}

	static class EchoReactor {
		TrafficHandler handler;
		ConnectionHandler acceptor;
		ServerSocket serverSocket;
		Socket clientSocket;

		int socketNumber;

		PrintWriter out;
		BufferedReader in;

		public EchoReactor() throws IOException {
			this.socketNumber = 80;
			serverSocket = new ServerSocket(this.socketNumber);
		}

		public EchoReactor(int socketNumber) throws IOException {
			this.socketNumber = socketNumber;
			serverSocket = new ServerSocket(this.socketNumber);
		}

		// Registers a handler that will treat the incoming messages from the
		// client
		public void registerTrafficHandler(TrafficHandler handler) {
			this.handler = handler;
			handler.setReactor(this);
		}

		public void registerConnectionHandler(ConnectionHandler handler) {
			this.acceptor = handler;
			acceptor.setReactor(this);
		}
		
		public void clientAccepted (Socket clientSocket){
			this.clientSocket = clientSocket;
			
		}
		
		
		
		

		// start accepting and handling incoming events
		public void handleEvents() {

			while (true) {
				accept();
				String fromClient = "Nothing received from client";
				String toClient = "Nothing to send to client";
				try {
					while (true) {
						fromClient = in.readLine();
						// The handleEvent of the EchoHandler is called, which
						// returns a TrafficEvent, from which we can get
						// resulting
						// message to send back to the client
						toClient = handler.handleEvent(
								new MyTrafficEvent(fromClient)).getMsg();
						out.println(toClient);
						if (fromClient.trim().toLowerCase().equals("shutdown") || fromClient.trim().toLowerCase().equals("bye"))
							break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Client disconnected");
				if (fromClient.trim().toLowerCase().equals("shutdown")){
					close();
					break;
				}
			}
			System.out.println("Server shutting down");
		}

		// Starts listening to the socket and sets up the required in- and
		// output variables
		private void accept() {
			
			try {
				// The handleEvent of the acceptor is called, which returns a
				// MyAcceptEvent, from which we can get the connected
				// ClientSocket
				acceptor.handleEvent(serverSocket);
				in = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void close() {

			try {
				System.out.println("Client disconnected");
				out.close();
				in.close();
				acceptor.close();
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// The handler that takes care of setting up the listening socket and
	// accepting the client connection.
	private static class AcceptHandler extends ConnectionHandler {
		Socket clientSocket;
		
		// This method does the actual work of accepting the client socket
		public void handleEvent(ServerSocket serverSocket) {
			try {
				System.out.println("Starting to listen");
				clientSocket = serverSocket.accept();
				// Pass the connected ClientSocket back to the Reactor
				reactor.clientAccepted(clientSocket);
				System.out.println("Client accepted");
			} catch (IOException e1) {
				e1.printStackTrace();
			}			
		}

		public void close() {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// The event used by the EchoHandler. It encapsulates the String message.
	private static class MyTrafficEvent implements MyAbstractEvent {
		String msg;

		public MyTrafficEvent(String msg) {
			this.msg = msg;
		}

		public String getMsg() {
			return msg;
		}

	}
}
