import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HuffmanThreadPool {
	static final String FILENAME = "US_Constitution.txt";

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Threading with pool implements Runnable.");
		System.out.println("Reading file...");
		Charset charset = Charset.forName("US-ASCII");

		byte[] encoded = Files.readAllBytes(Paths.get(FILENAME));
		String text = new String(encoded, charset);

		HuffmanPool compression = new HuffmanPool(text, 0);
		System.out.println("Result:");

		compression.compress();

		int numPro = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(numPro);

		long startTime = System.currentTimeMillis();

		for (int i = 1; i < numPro; i++) {
			Runnable r = compression;
			pool.execute(r);
		}

		while (!compression.shutdownPool) {
			// Wait for all tasks done
		}
		pool.shutdown();
		long endTime = System.currentTimeMillis();
		System.out.println(compression.runTime("Run time for generating compressed code: ", startTime, endTime));
		compression.printReport();
	}
}

class HuffmanPool implements Runnable {
	static final int LINE_FEED = 10;
	static final int ENTER = 13;
	static final int SPACE = 32;

	private HashMap<Character, Integer> frequency = new HashMap<>();
	private HashMap<Character, String> compressedCode = new HashMap<>();
	private ArrayList<Character> sorted;
	private char[] letters;
	private int numChars;
	private String fileContent;
	private String bits;
	private long startTime;
	private long endTime;

	private ArrayList<HuffmanNode> allNode = new ArrayList<HuffmanNode>();
	public boolean shutdownPool = false;

	public HuffmanPool(String text, int op) {
		this.fileContent = text;
//		this.fileContent = text.toLowerCase();
		bits = "";
		letters = fileContent.toCharArray();
	}

	private class HuffmanNode {
		char c;
		int count;

		HuffmanNode left;
		HuffmanNode right;

		// new part
		HuffmanNode parent;
		boolean isLeft;
	}

	private void createFrequency() {
		int count = 0;

		for (char letter : letters) {
			count = 0;
			if ((int) letter != LINE_FEED) {
				if (frequency.get(letter) == null) {
					count = 1;
				} else {
					count = frequency.get(letter) + 1;
				}
				frequency.put(letter, count);
			}
		}
	}

	public void compress() {
		createFrequency();

//		Comparator<HuffmanNode> compareNode = (HuffmanNode n1, HuffmanNode n2) -> (n1.count - n2.count);
		PriorityQueue<HuffmanNode> pq = new PriorityQueue<>(frequency.size(),
				(HuffmanNode n1, HuffmanNode n2) -> (n1.count - n2.count));

		for (char c : frequency.keySet()) {
			HuffmanNode node = new HuffmanNode();
			node.c = c;
			node.count = frequency.get(c);
			node.left = null;
			node.right = null;

			pq.add(node);
			allNode.add(node);
		}

		HuffmanNode root = null;

		// Create the tree
		startTime = System.currentTimeMillis();
		while (pq.size() > 1) {
			HuffmanNode e1 = pq.peek();
			pq.poll();
			HuffmanNode e2 = pq.peek();
			pq.poll();

			HuffmanNode newNode = new HuffmanNode();
			newNode.count = e1.count + e2.count;
			newNode.c = '*';

			newNode.left = e1;
			newNode.right = e2;

			e1.parent = newNode;
			e1.isLeft = true;
			e2.parent = newNode;
			e2.isLeft = false;

			root = newNode;
			pq.add(newNode);
		}
		endTime = System.currentTimeMillis();
		System.out.println(printRunTime("Running time for creating tree: "));

		root.parent = null;
		numChars = root.count;

		// Generate compressed code for each char
//		startTime = System.currentTimeMillis();
//		createCompressedCode(root, "");
//		createCompressedCodeBackward();
//		endTime = System.currentTimeMillis();
//		System.out.println(printRunTime("Run time for generating compressed code: "));

	}

	public void run() {
		createCompressedCodeBackward();
	}

	public void createCompressedCodeBackward() {
		for (int i = 0; i < allNode.size(); i++) {
			HuffmanNode currentNode = allNode.get(i);
			String code = "";

			while (currentNode.parent != null) {
				if (currentNode.isLeft) {
					code += 0;
				} else {
					code += 1;
				}
				currentNode = currentNode.parent;
			}

			compressedCode.put(allNode.get(i).c, code);
		}
		shutdownPool = true;
	}

//	public void createCompressedCode(HuffmanNode node, String code) {
//		if (node.left == null && node.right == null) {
//			compressedCode.put(node.c, code);
//			return;
//		}
//
//		createCompressedCode(node.left, code + "0");
//		createCompressedCode(node.right, code + "1");
//	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void sortHashMap(HashMap hashMap) {
		sorted = new ArrayList<>(hashMap.keySet());
		Collections.sort(sorted);
	}

	public void printReport() {
//		String bits = "";
		// Print more details
//		sortHashMap(compressedCode);
//
//		for (char c : sorted) {
//			if ((int) c == ENTER) {
//				System.out.println(" " + compressedCode.get(c));
//			} else {
//				System.out.println(c + " " + compressedCode.get(c));
//			}
//		}
//
		System.out.println("*****");
		System.out.println("Number of characters: " + numChars);

		// Compress the file
		startTime = System.currentTimeMillis();
		for (char c : letters) {
			if ((int) c != LINE_FEED) {
				bits += compressedCode.get(c);
			}
		}

		endTime = System.currentTimeMillis();
		System.out.println("Number of bits: " + bits.length());
		System.out.println("Number of bytes: " + bits.length() / 8);
		System.out.println(printRunTime("Run time for encoding file: "));
//		System.out.println(bits);
	}

	private String printRunTime(String message) {
		return runTime(message, startTime, endTime);
	}

	public String runTime(String message, long startTime, long endTime) {
		long runTime = endTime - startTime;

		if (runTime > 10_000) {
			return message + runTime / 1000.0 + " seconds.";
		} else {
			return message + runTime + " miliseconds.";
		}
	}
}
