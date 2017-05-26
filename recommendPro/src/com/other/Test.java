package com.other;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class Test {
	public static void main(String[] args) {
		double[][] d = new double[][]{{3,4,3,1},{1,3,2,6},{2,4,1,5},{3,3,5,2}};
		SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(d));
		svd.getU().print(2, 6);
		System.out.println();
		svd.getV().print(2, 6);
		System.out.println();
		
		double[][] sigular = new double[4][4]; 
		double[] singularValue = svd.getSingularValues();
		for (int i = 0; i < singularValue.length; i++) {
			sigular[i][i]=singularValue[i];
		}
		
		Matrix sigularMatrix = new Matrix(sigular);
		
		svd.getU().times(sigularMatrix).times(svd.getV().transpose()).print(2, 6);
		
	}
}
