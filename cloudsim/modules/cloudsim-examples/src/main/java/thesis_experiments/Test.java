package thesis_experiments;

public class Test {

	public void printBin(String soFar, int iterations) {
	    if(iterations == 0) {
	        System.out.println(soFar);
	    }
	    else {
	        printBin(soFar + "1", iterations - 1);
	        printBin(soFar + "2", iterations - 1);
	        printBin(soFar + "3", iterations - 1);
	        printBin(soFar + "4", iterations - 1);
	    }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Test t = new Test();
		t.printBin("", 3);
	}

}
