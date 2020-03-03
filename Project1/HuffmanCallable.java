import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HuffmanCallable {
	static final String FILENAME = "US_Constitution.txt";

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		System.out.println("Threading with Callable.");
		System.out.println("Reading file...");
		Charset charset = Charset.forName("US-ASCII");

		byte[] encoded = Files.readAllBytes(Paths.get(FILENAME));
		String text = new String(encoded, charset);

		HuffmanRun compression = new HuffmanRun(text, 0);
		System.out.println("Result:");

		compression.compress();
		compression.printReport();
	}
}

class CompressCallable implements Callable<String> {
	private char[] letters;
	private HashMap<Character, String> compressedCode = new HashMap<>();
	private int start;
	private int end;

	public CompressCallable(char[] letters, HashMap<Character, String> mapping, int start, int end) {
		this.letters = letters;
		this.compressedCode = mapping;
		this.start = start;
		this.end = end;
	}

	public String call() throws Exception {
		String codeResult = "";

		for (int i = start; i < end; i++) {
			char c = letters[i];
			if ((int) c != HuffmanRun.LINE_FEED) {
				codeResult += compressedCode.get(c);
			}
		}

		return codeResult;
	}
}

class HuffmanRun {
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

	public HuffmanRun(String text, int op) {
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

			root = newNode;
			pq.add(newNode);
		}
		endTime = System.currentTimeMillis();
		System.out.println(printRunTime("Running time for creating tree: "));

		numChars = root.count;

		// Generate compressed code for each char
		startTime = System.currentTimeMillis();
		createCompressedCode(root, "");
		endTime = System.currentTimeMillis();
		System.out.println(printRunTime("Run time for generating compressed code: "));

	}

	public void createCompressedCode(HuffmanNode node, String code) {
		if (node.left == null && node.right == null) {
			compressedCode.put(node.c, code);
			return;
		}

		createCompressedCode(node.left, code + "0");
		createCompressedCode(node.right, code + "1");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void sortHashMap(HashMap hashMap) {
		sorted = new ArrayList<>(hashMap.keySet());
		Collections.sort(sorted);
	}

	public void printReport() throws InterruptedException, ExecutionException {
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
//		for (char c : letters) {
//			if ((int) c != LINE_FEED) {
//				bits += compressedCode.get(c);
//			}
//		}
		int mid = (letters.length - 1) / 2;

		ExecutorService executor = Executors.newFixedThreadPool(4);

		Future firstPart = executor.submit(new CompressCallable(letters, compressedCode, 0, mid / 2));
		Future secondPart = executor.submit(new CompressCallable(letters, compressedCode, mid / 2, mid));
		Future thirdPart = executor.submit(new CompressCallable(letters, compressedCode, mid, mid + (mid / 2)));
		Future fourthPart = executor
				.submit(new CompressCallable(letters, compressedCode, mid + (mid / 2), letters.length));

//		String result = firstPart.get().toString();

		bits = firstPart.get().toString() + secondPart.get().toString() + thirdPart.get().toString()
				+ fourthPart.get().toString();
		executor.shutdown();
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
