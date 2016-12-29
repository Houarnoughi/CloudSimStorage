package thesis_experiments;


public class GetAllPossibilities {
	int[] j_array;

	public GetAllPossibilities(int n) {
		j_array = new int[n];
	}

	public void getMinPlacementPlan(int nbVm, int nbDev) {
		if (nbVm <= 0) {
			for (int i=0; i<j_array.length; i++){
				System.out.print("C_"+i+"_"+j_array[i]+" ");
			}
			System.out.println();
		} else {
			for(int j=0; j<nbDev; j++){
				j_array[nbVm - 1] = j;
				getMinPlacementPlan(nbVm - 1, nbDev);
			}
		}
	}

	public static void main(String[] args) throws java.lang.Exception {
		int nbVm = 3;
		int nbDev = 2;
		GetAllPossibilities brute_force = new GetAllPossibilities(nbVm);
		brute_force.getMinPlacementPlan(nbVm, nbDev);
	}
}
