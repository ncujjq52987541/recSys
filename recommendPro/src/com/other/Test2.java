package com.other;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class Test2 {
	public static void main(String[] args) {
		double[][] d = new double[][]{{0,1,0,1},{0,1,1,1},{1,0,1,0}};
		SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(d));
		svd.getU();
		System.out.println();
		svd.getV().print(2, 2);
		System.out.println();
		
		
//		svd.getU().times(sigularMatrix).times(svd.getV().transpose()).print(2, 6);
		
	}
}
