import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class HuffmanForkJoin {
	static final String FILENAME = "US_Constitution.txt";

	public static void main(String[] args) throws IOException {

		System.out.println("Threading with fork join.");
		System.out.println("Reading file...");
		Charset charset = Charset.forName("US-ASCII");

		byte[] encoded = Files.readAllBytes(Paths.get(FILENAME));
		String text = new String(encoded, charset);

		HuffmanJoin compression = new HuffmanJoin(text, 0);
		System.out.println("Result:");

		compression.compress();
		compression.printReport();
	}
}

class HuffmanNode {
	char c;
	int count;

	HuffmanNode left;
	HuffmanNode right;
}

class Compress extends RecursiveAction {
	private static HashMap<Character, String> compressedCode = new HashMap<>();
	HuffmanNode node;
	String code;

	public Compress(HuffmanNode node, String code) {
		this.node = node;
		this.code = code;
	}

	public static HashMap<Character, String> getCompressedCode() {
		return compressedCode;
	}

	@Override
	protected void compute() {
		if (node.left == null && node.right == null) {
			compressedCode.put(node.c, code);
		} else {
			String newLeftCode = code + "0";
			String newRightCode = code + "1";
			invokeAll(new Compress(node.left, newLeftCode), new Compress(node.right, newRightCode));
		}
	}
}

class CompressFile extends RecursiveTask<String> {
	private char[] letters;
	private int start, end, threshold;
	private String result = "";

	public CompressFile(char[] letters, int start, int end, int threshold) {
		this.letters = letters;
		this.start = start;
		this.end = end;
		this.threshold = threshold;
	}

	@Override
	protected String compute() {
		if (end - start <= threshold) {
			for (int i = start; i < end; i++) {
				char c = letters[i];
//				System.out.print(c);
				if ((int) c != HuffmanJoin.LINE_FEED) {
					result += Compress.getCompressedCode().get(c);
				}
			}
//			System.out.println();
			return result;
		} else {
			int mid = (end - start) / 2;
			CompressFile left = new CompressFile(letters, start, mid, threshold);
			CompressFile right = new CompressFile(letters, mid, end, threshold);
			right.fork();
			return left.compute() + right.join();
		}
	}
}

class HuffmanJoin {
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

	public HuffmanJoin(String text, int op) {
		this.fileContent = text;
		bits = "";
		letters = fileContent.toCharArray();
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
//		createCompressedCode(root, ""); //old method		
		ForkJoinPool fj = new ForkJoinPool();
		Compress compress = new Compress(root, "");

		startTime = System.currentTimeMillis();
		fj.invoke(compress);
		fj.shutdown();
		endTime = System.currentTimeMillis();
		System.out.println(printRunTime("Run time for generating compressed code: "));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void sortHashMap(HashMap hashMap) {
		sorted = new ArrayList<>(hashMap.keySet());
		Collections.sort(sorted);
	}

	public void printReport() {

		compressedCode = Compress.getCompressedCode();
		sortHashMap(compressedCode);
//		// Print more details
//		for (char c : sorted) {
//			if ((int) c == ENTER) {
//				System.out.println(" " + compressedCode.get(c));
//			} else {
//				System.out.println(c + " " + compressedCode.get(c));
//			}
//		}

		System.out.println("*****");
		System.out.println("Number of characters: " + numChars);

		// Compress the file
		// Old method
//		for (char c : letters) {
//			if ((int) c != LINE_FEED) {
//				bits += compressedCode.get(c);
//			}
//		}

		int threshold = letters.length / 2;

		startTime = System.currentTimeMillis();
		ForkJoinPool fork = new ForkJoinPool().commonPool();
		bits = fork.invoke(new CompressFile(letters, 0, letters.length, threshold));

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
