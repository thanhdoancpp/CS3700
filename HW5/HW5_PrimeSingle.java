public class HW5_PrimeSingle {

	public static void main(String[] args) {
		int max = 1_000_000;
		int[] numArr = new int[max - 1];
		int countPrint = 0;
		int tempPrime;

		for (int i = 0; i < numArr.length; i++) {
			numArr[i] = i + 2;
		}

		long startTime = System.currentTimeMillis();

		System.out.println("Prime num through " + max);

		for (int i = 0; i < numArr.length; i++) {
			tempPrime = numArr[i];
			
			if (tempPrime == -1) {
				continue;
			}

			System.out.print(tempPrime + " ");
			countPrint++;

			if (countPrint == 10) {
				System.out.println();
				countPrint = 0;
			}

			for (int j = i + 1; j < numArr.length; j++) {
				if (numArr[j] % tempPrime == 0) {
					numArr[j] = -1;
				}

			}
		}

		System.out.println();

		long endTime = System.currentTimeMillis();

		System.out.println("Run time: " + (endTime - startTime) / 1_000.0 + " seconds.");
	}
}
