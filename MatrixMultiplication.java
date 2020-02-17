import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MatrixMultiplication {

	public static final int MIN_ROW = 3;
	public static final int MAX_ROW = 100;
	public static final int MIN_COL = 3;
	public static final int MAX_COL = 100;
	public static final int MAX_RANDOM = 100;
	public static final int RUNNING = 5;
	
	public static void main(String[] args) {
		int[] threads = {1, 2, 4, 8};
		
		for (int round = 1; round <= RUNNING; round++)
		{
			System.out.println("----- Round " + round + " -----");
			
			Random r = new Random();
			
			int m = r.nextInt(MAX_ROW) + MIN_ROW;
			int n = r.nextInt(MAX_COL) + MIN_COL;
			int p = m;
			
			float[][] a = randomMatrix(m, n, MAX_RANDOM);
			float[][] b = randomMatrix(n, p, MAX_RANDOM);
			float[][] c = new float[m][p];
			
			System.out.println("Running with " + m + "x" + n + " and " + n + "x" + p + " matrix.");
			for(int i = 0; i < threads.length; i++)
			{	
				float[][] a1 = a.clone();
				float[][] b1 = b.clone();
				float[][] c1 = c.clone();
				
				long startTime = System.currentTimeMillis();
				matMul(a1, b1, c1, m, n, p, threads[i]);
				long endTime = System.currentTimeMillis();
				
				System.out.println("Time with " + threads[i] + " thread: " + ((endTime - startTime) / 1000.0) + " seconds.");
			}
		}
	}

	public static void matMul(float[][] a, float[][] b, float[][] c, int m, int n, int p, int numThread)
	{		
		ExecutorService executor = Executors.newFixedThreadPool(numThread);
		
		for(int i = 0; i < m; i++)
		{
			for (int j = 0; j < p; j++)
			{
				for(int k = 0; k < n; k++)
				{
					final int x = i;
					final int y = j;
					final int z = k;
					executor.execute(() -> c[x][y] += a[x][z] * b[z][y]);
				}
			}
		}
		
		executor.shutdown();
		
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

//		printMatrix(c);
	}
	
	public static void printMatrix(float[][] c)
	{
		for (int i = 0; i < c.length; i++)
		{
			for (int j = 0; j < c[0].length; j++)
			{
				System.out.print(c[i][j] + "\t");
			}
			System.out.println();
		}
	}
	
	public static float[][] randomMatrix(int row, int col, float maxValue)
	{
		float[][] matrix = new float[row][col];
		
		for(int i = 0; i < row; i++)
		{
			for(int j = 0; j < col; j++)
			{
				Random r = new Random();
				matrix[i][j] = r.nextFloat() * maxValue;
			}
		}
		
		return matrix;
	}
}
