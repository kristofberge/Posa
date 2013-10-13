import java.util.Arrays;

public class DiningPhilosophers {

	public static void main(String[] args) {
		int numberOfPhilosophers = 5; // # of philosophers at the table
		int meals = 5; // # of times they will eat before finishing
		
		System.out.println("Dinner is starting!\n");

		//Initialize the table (which is the Monitor)
		TableMonitor table = new TableMonitor(numberOfPhilosophers); 

		//Initialize the philosopher Thread array
		Thread philosophers[] = new Thread[numberOfPhilosophers];
		
		for (int i = 0; i < numberOfPhilosophers; i++) {
			philosophers[i] = new Thread(new Philosopher(i + 1, meals, table));
		}

		//Start the philosopher Threads
		for (int i = 0; i < numberOfPhilosophers; i++) { 
			philosophers[i].start();
		}

		//Wait until all of the philosophers are done eating
		while (!table.allDone()) { 
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\nDinner is over!");
	}

	private static class Philosopher implements Runnable {
		private int num, timesToEat;
		private TableMonitor table;

		public Philosopher(int num, int timesToEat, TableMonitor table) {
			this.num = num;
			this.timesToEat = timesToEat;
			this.table = table;
		}

		public int getNum() {
			return num;
		}

		public void run() {
			for (int i = 0; i < timesToEat; i++) {
				table.pickUp(num);
				eat();
				table.putDown(num);
			}
			table.iAmDone(); //let the table know that you are done eating
		}

		private void eat() {
			System.out.println("Philosopher " + num + " is eating.");
		}

	}

	private static class TableMonitor {
		private int numberOfPhilosophers;
		private int doneEating = 0;

		private enum State {
			THINKING, EATING
		}

		private State states[];

		public TableMonitor(int numberOfPhilosophers) {
			this.numberOfPhilosophers = numberOfPhilosophers;
			states = new State[this.numberOfPhilosophers];
			Arrays.fill(states, State.THINKING); //Initially all of the philosophers are Thinking
		}

		public void iAmDone() {
			doneEating++;
		}

		public boolean allDone() {
			return doneEating == numberOfPhilosophers;
		}

		public synchronized void pickUp(int philNum) {

			//Check if any of the adjacent philosophers are Eating
			while (states[(philNum - 1) % numberOfPhilosophers].equals(State.EATING)
					|| states[(philNum + 1) % numberOfPhilosophers].equals(State.EATING)) {

				try {
					//Wait if any of the adjacent philosophers are eating
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//if neither of the adjacent philosophers are eating, pick up both left and right chopstick and start eating
			System.out.println("Philosopher " + philNum + " picks up the right chopstick.");
			System.out.println("Philosopher " + philNum + " picks up the left chopstick.");
			states[philNum-1] = State.EATING;
		}

		public synchronized void putDown(int philNum) {
			//put down both chopsticks and start thinking
			states[philNum-1] = State.THINKING;
			System.out.println("Philosopher " + philNum + " puts down the right chopstick.");
			System.out.println("Philosopher " + philNum + " puts down the left chopstick.");
			notifyAll();
		}
	}

}
