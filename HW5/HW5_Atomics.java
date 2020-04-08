import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class HW5_Atomics {
	static Machine machine = new Machine();

	public static void main(String[] args) throws InterruptedException {
		final int PRODUCER_SIZE = 100;
		final long CONSUME_TIME = 1_000; // 1 second
		long startTime = System.currentTimeMillis();

		ArrayList<Thread> list = new ArrayList<Thread>();
		int noProducer = 2;
		int noConsumer = 5;

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
				while (!machine.getEmpty()) {
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
		AtomicInteger producerSize = new AtomicInteger(-1);

		AtomicBoolean empty = new AtomicBoolean(false);
		private static ArrayList<String> itemsArr = new ArrayList<String>(BUFFER_SIZE);
		AtomicReference<ArrayList<String>> items = new AtomicReference<ArrayList<String>>(itemsArr);

		
		public void add(String item) throws InterruptedException {
			while(items.get().size() == BUFFER_SIZE)
			{
				Thread.sleep(5);
			}
			
			ArrayList<String> old = (ArrayList<String>) itemsArr.clone();
//			itemsArr.add(item);
			
			
			if(!items.compareAndSet(old, itemsArr))
			{
				itemsArr.add(item);
			}
		}

		public String remove() throws InterruptedException {
			String item = null;
			for (int i = 0; i < 10; i++)
			{
				if(items.get().size() == 0)
				{
					Thread.sleep(5);
				}
				else
				{
					break;
				}
			}
			
			
			if (items.get().size() == 0) {
				empty.set(true);
				
				return item;
			}
			
			ArrayList<String> old = (ArrayList<String>) itemsArr.clone();
			if(!items.compareAndSet(old, itemsArr))
			{
//				item = items.get().remove(0);
				item = itemsArr.remove(0);
			}
//			item = items.get().remove(0);
			
			return item;
		}
		
		public void setTotalProduct(int num) {
			this.producerSize.set(num);
		}

		public boolean getEmpty() {
			return empty.get();
		}
	}
}
