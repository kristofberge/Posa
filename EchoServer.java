import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class EchoServer {
	static int portnumber;
	static int processingUnits;

	
	public static void main(String[] args) {
		portnumber = Integer.parseInt(args[0]);
		processingUnits = Runtime.getRuntime().availableProcessors();

		ThreadPool tasks = new ThreadPool(processingUnits + 1);
		RequestQueue requestQueue = RequestQueue.getInstance();
		tasks.activateThreads();

		Reactor reactor = Reactor.getInstance();

		reactor.setRequestQueue(requestQueue);
		reactor.reactorLoop();
	}

	private static class Reactor {
		private static Reactor reactor;
		private RequestQueue requestQueue;
		private ServerSocket serverSocket;

		private static enum EventType {
			ACCEPT, IN, OUT, DISCONNECT
		}

		private Reactor() {
			try {
				serverSocket = new ServerSocket(8080);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public static Reactor getInstance() {
			if (reactor == null) {
				reactor = new Reactor();
			}
			return reactor;
		}

		public void setRequestQueue(RequestQueue requestQueue) {
			this.requestQueue = requestQueue;
		}

		public void clientAccepted(Socket client) {
			System.out.println("client accepted");
			Event e = new Event(new Client(client), EventType.ACCEPT);
			requestQueue.enqueue(e);
		}

		public void readyToRead(Client client, BufferedReader input) {
			System.out.println("ready to read");
			client.setInput(input);
			requestQueue.enqueue(new Event(client, EventType.IN));
		}

		public void sendToClient(String message, Client client) {
			System.out.println("msg sent");
			try {
				PrintWriter output = new PrintWriter(client.getSocket()
						.getOutputStream(), true);
				client.setOutput(output);
				requestQueue.enqueue(new Event(message, client, EventType.OUT));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		public void clientDisconnect(Client client) {
			System.out.println("Client disconnect");
			requestQueue.enqueue(new Event(client, EventType.DISCONNECT));
		}

		public void reactorLoop() {
			Acceptor acceptor = new Acceptor(serverSocket, reactor);
			acceptor.handleInput();
		}
	}

	private static class Client {
		BufferedReader input;
		PrintWriter output;
		Socket socket;

		public Client(Socket socket) {
			this.socket = socket;
		}

		public PrintWriter getOutput() {
			return output;
		}

		public void setOutput(PrintWriter output) {
			this.output = output;
		}

		public Socket getSocket() {
			return socket;
		}

		public BufferedReader getInput() {
			return input;
		}

		public void setInput(BufferedReader input) {
			this.input = input;
		}

	}

	private static class Event {
		String msg;
		Client client;
		Reactor.EventType eventType;

		public Event(Client client, EchoServer.Reactor.EventType eventType) {
			this.client = client;
			this.eventType = eventType;
		}

		public Event(String msg, Client client,
				EchoServer.Reactor.EventType eventType) {
			this.msg = msg;
			this.client = client;
			this.eventType = eventType;
		}

		public Client getClient() {
			return client;
		}

		public String getMsg() {
			return msg;
		}

		public Reactor.EventType getEventType() {
			return eventType;
		}
	}

	private static class ThreadPool {
		Thread[] threads;
		RequestQueue requestQueue;

		public ThreadPool(int numberOfThreads) {
			threads = new Thread[numberOfThreads];
			requestQueue = RequestQueue.getInstance();

		}

		public void activateThreads() {

			for (Thread t : threads) {
				t = new Thread(new Runnable() {

					@Override
					public void run() {

						while (true) {
							Event event = requestQueue.getNextEvent();
							System.out.println(event.getEventType()
									+ " event taken from queue by thread "
									+ Thread.currentThread().getId());
							switch (event.getEventType()) {
							case ACCEPT:
								EchoAcceptor echoAcceptor = new EchoAcceptor(
										event.getClient(), Reactor
												.getInstance());
								echoAcceptor.handleInput();
								break;
							case IN:
								EchoServiceHandler handler = new EchoServiceHandler(
										event.getClient(), Reactor
												.getInstance());
								handler.handleInput();
								break;
							case OUT:
								EchoSender sender = new EchoSender(event
										.getClient(), Reactor.getInstance());
								sender.setMessage(event.getMsg());
								sender.handleInput();
								break;
							case DISCONNECT:
								ClientDisconnect disconnect = new ClientDisconnect(
										event.getClient(), Reactor
												.getInstance());
								disconnect.handleInput();
								break;
							}
						}
					}
				});
				t.start();
			}
		}
	}

	private static class RequestQueue {
	
		private static RequestQueue me;
		private ArrayBlockingQueue<Event> queue;
	
		private RequestQueue(int elements) {
			queue = new ArrayBlockingQueue<Event>(elements);
		}
	
		public static RequestQueue getInstance() {
			if (me == null)
				me = new RequestQueue(processingUnits * 2);
	
			return me;
		}
	
		public void enqueue(Event event) {
			try {
				queue.put(event);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
		public synchronized Event getNextEvent() {
			Event event = null;
			try {
				event = (Event) queue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return event;
		}
	}

	private static abstract class ServiceHandler<HANDLE> {
		HANDLE handle;
		Reactor reactor;
	
		public ServiceHandler(HANDLE handle, Reactor reactor) {
			this.handle = handle;
			this.reactor = reactor;
		}
	
		public abstract void handleInput();
	}

	private static class ClientDisconnect extends ServiceHandler<Client> {

		public ClientDisconnect(Client handle, Reactor reactor) {
			super(handle, reactor);
		}

		@Override
		public void handleInput() {
			try {
				handle.getOutput().close();
				handle.getInput().close();
				handle.getSocket().close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	private static class EchoSender extends ServiceHandler<Client> {

		String message;

		public void setMessage(String message) {
			this.message = message;
		}

		public EchoSender(Client handle, Reactor reactor) {
			super(handle, reactor);
		}

		public void handleInput() {
			if (handle.getOutput() == null) {
				try {
					handle.setOutput(new PrintWriter(handle.getSocket()
							.getOutputStream(), true));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			handle.getOutput().println(message);
		}
	}

	private static class EchoAcceptor extends ServiceHandler<Client> {

		public EchoAcceptor(Client client, Reactor reactor) {
			super(client, reactor);
		}

		@Override
		public void handleInput() {
			BufferedReader input;
			try {

				input = new BufferedReader(new InputStreamReader(handle
						.getSocket().getInputStream()));

				reactor.readyToRead(handle, input);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class EchoServiceHandler extends ServiceHandler<Client> {
	
		public EchoServiceHandler(Client handle, Reactor reactor) {
			super(handle, reactor);
			// TODO Auto-generated constructor stub
		}
	
		@Override
		public void handleInput() {
			String fromClient = "Nothing received from client ";
			BufferedReader input = handle.getInput();
	
			while (true) {
	
				try {
					fromClient = input.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (fromClient.trim().toLowerCase().equals("bye")
						|| fromClient.trim().toLowerCase().equals("shutdown")) {
					reactor.clientDisconnect(handle);
					break;
				} else {
					reactor.sendToClient(generateAnswer(fromClient), handle);
				}
			}
		}
	
		private String generateAnswer(String input) {
			return "Answer: ," + input + "' was generated by thread "
					+ Thread.currentThread().getId();
		}
	
	}

	private static class Acceptor extends ServiceHandler<ServerSocket> {

		public Acceptor(ServerSocket handle, Reactor reactor) {
			super(handle, reactor);
		}

		@Override
		public void handleInput() {
			accept();
		}

		private void accept() {
			Socket client = null;
			try {
				while (true) {
					client = handle.accept();
					reactor.clientAccepted(client);
				}
			} catch (IOException e) {

				e.printStackTrace();
			}

		}

	}
}
