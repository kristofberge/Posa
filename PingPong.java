import java.util.concurrent.Semaphore;

public class PingPong {
	static boolean pingsTurn = false;
	static boolean pongsTurn = false;

	public static void main(String[] args) {

		final Semaphore sem = new Semaphore(1, true);

		Thread ping = new Thread() {
			public void run() {
				for (int i = 0; i < 3; i++) {
					sem.acquireUninterruptibly();

					System.out.println("Ping!");
					sem.release();
					pingsTurn = false; 
					pongsTurn = true;
					if(i!=2){ //check to avoid infinite loop on last iteration
						while (!pingsTurn) {} //wait until it's my turn
					}
				}
			}
		};

		Thread pong = new Thread() {
			public boolean notMyTurn = true;

			public void run() {
				for (int i = 0; i < 3; i++) {
					sem.acquireUninterruptibly();

					System.out.println("Pong!");
					sem.release();
					pongsTurn = false;
					pingsTurn = true;
					if(i!=2){ //check to avoid infinite loop on last iteration
						while (!pongsTurn) {} //wait until it's my turn
					}
				}
			}
		};

		System.out.println("Ready... Set... Go!");

		ping.start();
		pong.start();

		while (ping.isAlive() || pong.isAlive()) {} // wait until both loops are finisheds

		System.out.println("Done!");
	}
}
