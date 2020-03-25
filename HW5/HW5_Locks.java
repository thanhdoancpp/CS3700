import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HW5_Locks {
	static Machine machine = new Machine();

	public static void main(String[] args) throws InterruptedException {
		final int PRODUCER_SIZE = 100;
		final long CONSUME_TIME = 1_000; // 1 second
		long startTime = System.currentTimeMillis();

		ArrayList<Thread> list = new ArrayList<Thread>();
		int noProducer = 5;
		int noConsumer = 2;

		machine.setTotalProduct(noProducer * PRODUCER_SIZE);
		for (int i = 1; i <= noProducer; i++) {
			Thread p = new Thread(new Producer(PRODUCER_SIZE), "Producer " + i);
			p.start();
			list.add(p);
		}

		for (int i = 1; i <= noConsumer; i++) {
//			Thread c = new Thread(new Consumer((int)Math.random() *1000), "Consumer " + i);
			Thread c = new Thread(new Consumer(CONSUME_TIME), "Consumer " + i);
			c.start();
			list.add(c);
		}

		for (Thread t : list) {
			t.join();
		}

		long endTime = System.currentTimeMillis();
		System.out.println("Run time: " + (endTime - startTime) / 1_000.0 + " seconds.");
	}

	static String getName() {
		return Thread.currentThread().getName();
	}

	static class Producer implements Runnable {
		int maxItems;

		public Producer(int maxItems) {
			this.maxItems = maxItems;
		}

		@Override
		public void run() {
			for (int i = 1; i <= maxItems; i++) {
				System.out.println(getName() + " -- add: " + i);
				try {
					machine.add(getName() + " - " + i);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static class Consumer implements Runnable {
		long waitTime;
		int count;

		public Consumer(long consumeTime) {
			this.waitTime = consumeTime;
		}

		@Override
		public void run() {
			try {
				while (!machine.empty) {
					String item = machine.remove();
					if (item != null) {
						System.out.println("*****" + getName() + " -- remove: " + item);
						Thread.sleep(waitTime);
						count++;
					}
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} finally {
				System.out.println("\t" + getName() + " consumes: " + count + " items.");
			}

		}
	}

	static class Machine {
		private static final int BUFFER_SIZE = 10;
		private int producerSize = -1;
		private boolean empty = false;

		private static ArrayList<String> items = new ArrayList<String>(BUFFER_SIZE);
		private static Lock lock = new ReentrantLock();

		private static Condition add = lock.newCondition();
		private static Condition remove = lock.newCondition();

		public void add(String item) throws InterruptedException {
			lock.lock();
			try {
				while (items.size() == BUFFER_SIZE) {
					remove.await();
				}

				items.add(item);
				add.signalAll();
			} finally {
				lock.unlock();
			}
		}

		@SuppressWarnings("finally")
		public String remove() throws InterruptedException {
			String item = null;
			lock.lock();
			try {
				while (items.isEmpty()) {
//					add.await();
					if (!add.await(100, TimeUnit.MILLISECONDS)) {
						break;
					}
				}

				item = items.remove(0);
				producerSize--;

				if (producerSize == 0) {
					empty = true;

				} else {
					remove.signalAll();
				}

			} finally {
				lock.unlock();
				return item;
			}
		}

		public void setTotalProduct(int num) {
			this.producerSize = num;
		}

		public boolean getEmpty() {
			return empty;
		}
	}
}
