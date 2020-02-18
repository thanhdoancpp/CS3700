import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

enum Color {
	Red, Green, Blue, Orange;
}

class Washer extends Thread {
	Matching m;
	
	public Washer(Matching m, String name)
	{
		this.m = m;
		this.setName(name);
	}
	
	public void run()
	{	
		while (true)
		{
			if(!m.waitForWashing.isEmpty())
			{
				Color c = m.waitForWashing.remove(0);
				
				System.out.println(this.getName() + ": Destroyed " + c.toString() + " socks.");
				synchronized (m) {
					m.notify();
				}
			}
			
			try {
				this.sleep(100);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}

class Matching extends Thread {
	ArrayList<Color> stock;
	int[] queue = new int[4]; //Red = 0, Green, Blue, Orange = 3
	int total = 0;
	ArrayList<Color> waitForWashing;
	
	public Matching(String name) {
		stock = new ArrayList<Color>();
		waitForWashing = new ArrayList<Color>();
		this.setName(name);
	}

	public void run() {
		while(true)
		{
			while (!stock.isEmpty())
			{
				matchingSocks();
				
				while(!waitForWashing.isEmpty())
				{
					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}		
			}
		}
		
		
		
	}
	
	public void matchingSocks()
	{
		Color c = stock.remove(0);
		switch(c)
		{
		case Red:{
			queue[0]++;
			sendToWasher(0, c);
			break;
		}
		case Green:{
			queue[1]++;
			sendToWasher(1, c);
			break;
		}
		case Blue:{
			queue[2]++;
			sendToWasher(2, c);
			break;
		}
		case Orange:{
			queue[3]++;
			sendToWasher(3, c);
			break;
		}
		default:
			System.out.println("Undifined");
		}
	}
	
	public void sendToWasher(int num, Color c)
	{
//		synchronized (waitForWashing) {
			if(queue[num] % 2 == 0)
			{
				queue[num] -= 2;
				waitForWashing.add(c);
				total+= 2;
				
				System.out.println(this.getName() + ": Send " + c + " Socks To Washer. Total socks " + total 
						+ ". Total inside queue " + waitForWashing.size());
			}
	}
	
	public void addSock(Color c) {
		synchronized (stock) {
			stock.add(c);
		}
	}

	public void waitForSock() {
		boolean isEmpty = true;

		while (isEmpty) {
			isEmpty = stock.isEmpty();
		}
	}
}

class Sock extends Thread {
	private int total = ThreadLocalRandom.current().nextInt(1, 10);
	private int count;
	public boolean flag = true;
	
	private Color colorCode;
	private Matching m;

	public Sock(Color colorCode, Matching m, String name) {
		count = 0;
		this.colorCode = colorCode;
		this.setName(name);
		this.m = m;
	}

	public synchronized void run() {
		while (count != total) {
			System.out.println(this.getName() + " total: " + total);
			increment();
			m.addSock(colorCode);
			System.out.println(this.getName() + ": Produced " + count + " of " + total + " " + this.getName());
		}

		System.out.println(this.getName() + " done!!!");
		flag = false;
	}

	private synchronized void increment() {
		count++;
	}
}

public class SockMachines {

	public static void main(String[] args) throws InterruptedException {
		int num = ThreadLocalRandom.current().nextInt(1, 101);

		Matching matching = new Matching("Matching Thread");
		matching.start();
		
		Washer washer = new Washer(matching, "Washer Thread");
		washer.start();
		
		Sock t1 = new Sock(Color.Red, matching, Color.Red.toString() + " Sock");
		t1.start();

		Thread t2 = new Sock(Color.Green, matching, Color.Green.toString() + " Sock");
		t2.start();

		Thread t3 = new Sock(Color.Blue, matching, Color.Blue.toString() + " Sock");
		t3.start();

		Thread t4 = new Sock(Color.Orange, matching, Color.Orange.toString() + " Sock");
		t4.start();

	}

}
