package com.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.management.RuntimeErrorException;

import Jama.Matrix;

public class ModelBaseCF extends AbstratMemoryBaseCF{

	@Override
	public void initUserOrItemSimMatrix() {
		
	}

	@Override
	public void simMatrixCompute(String similiar) {
		
	}
	private int latent=20;//隐类
	private float alpha=0.03f;
	private float lambda=0.01f;
	
	private Matrix preditRating;
	private double times(double[] a,double[] b){
		if(a.length!=b.length){
			throw new RuntimeException("a与b长度不等");
		}
		double sum=0;
		for (int i = 0; i < b.length; i++) {
			sum+=a[i]*b[i];
		}
		return sum;
	}
	public ModelBaseCF(String path,String splitSymbol){
		loadData(path, splitSymbol);
		Matrix rating = getUseItemRating();
//		Matrix preditRating = getUseItemRating().copy();
		double[][] uk = new double[rating.getRowDimension()][latent];
		double[][] ki = new double[latent][rating.getColumnDimension()];
		
		for (int i = 0; i < uk.length; i++) {
			for (int j = 0; j < uk[i].length; j++) {
				uk[i][j]=Math.random();
			}
		}
		for (int i = 0; i < ki.length; i++) {
			for (int j = 0; j < ki[i].length; j++) {
				ki[i][j]=Math.random();
			}
		}
		Matrix ukMatrix = new Matrix(uk);
		Matrix kiMatrix = new Matrix(ki);
		
//		ukMatrix.print(2, 2);
//		kiMatrix.print(2, 2);
		int num = 0;
		while(num++<20){
			System.out.println("正在进行第："+num+" 次迭代");
			for (int u = 0; u < rating.getRowDimension(); u++) {
				for (int i = 0; i < rating.getColumnDimension(); i++) {
					for (int k = 0; k < this.latent; k++) {
//						double temp_uk = uk[u][k];
//						double temp_ki = ki[k][i];
						double error = rating.getArray()[u][i]-times(uk[u],kiMatrix.getMatrix(0, kiMatrix.getRowDimension()-1, i, i).transpose().getArray()[0]);
//						System.out.println("error:"+error);
//						if(error.isNaN()){
//							System.out.println(num);
//							System.out.println("");
//							
//						}
//						Double t1 = new Double(uk[u][k]);
//						Double t2 = new Double(ki[k][i]);
						
						uk[u][k]=uk[u][k]+alpha*error*ki[k][i]-lambda*uk[u][k];
//						if(t1.isInfinite()||t2.isInfinite()){
//							System.out.println("");
//						}
						ki[k][i]=ki[k][i]+alpha*error*uk[u][k]-lambda*ki[k][i];
//						if(t1.isInfinite()||t2.isInfinite()){
//							System.out.println("");
//						}
//						System.out.println(uk[u][k]+","+uk[u][k]);
//						ukMatrix.print(2, 2);
//						kiMatrix.print(2, 2);
					}
				}
				
			}
//		p_uk = p_uk.plus(
//				((rating.minus(p_uk.times(q_ki))).times(q_ki).minus(p_uk.times(lambda))).times(alpha)
//				);
//		q_ki = q_ki.plus(
//				((rating.minus(p_uk.times(q_ki))).times(p_uk)).minus(q_ki.times(lambda)).times(alpha)
//				);
		}
//		userItemSimilar = new Matrix(new double[rating.getRowDimension()][rating.getColumnDimension()]);
		
		preditRating = ukMatrix.times(kiMatrix);
		preditRating.print(2, 2);
	}
	
	public List<Result> recommend(int userId,int k){
		int uindex = userIndex.get(userId);
		
		//拿到该用户的评分向量
		double[] rating = useItemRating.getMatrix(uindex, uindex, 0, useItemRating.getColumnDimension()-1).getArray()[0];
		
		double[] preRating = preditRating.getMatrix(uindex, uindex, 0, useItemRating.getColumnDimension()-1).getArray()[0];
		List<Result> finalResult = new ArrayList<Result>();
		for (int i = 0; i < rating.length; i++) {
			if(rating[i]==0){//仅对未评过分的推荐
				Result r = new Result();
				r.setItemId(indexItem.get(i));
				r.setSimDegree(preRating[i]);
				finalResult.add(r);
			}
		}
		Collections.sort(finalResult);
		if(finalResult.size()>k){
			return finalResult.subList(0, k);
		}else{
			return finalResult;
		}
	}
	
	public static void main(String[] args) {
		ModelBaseCF t = new ModelBaseCF("D:\\tmp\\recommend\\test1\\train.txt"," ");
		Matrix m = t.getUseItemRating();
		m.print(2, 2);
		t.preditRating.print(2, 2);
		List<Result> result = t.recommend(1, 10);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Result result2 = (Result) iterator.next();
			System.out.println(result2.getItemId()+","+result2.getSimDegree());
		}
	}
}
