package com.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



import Jama.Matrix;

public class ModelBaseCF extends AbstratMemoryBaseCF{

	@Override
	public void initUserOrItemSimMatrix() {
		
	}

	@Override
	public void simMatrixCompute(String similiar) {
		
	}
//	private int latent=20;//隐类
//	private float alpha=0.03f;
//	private float lambda=0.01f;
//	private int iteratorNums = 10;//迭代次数
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
	private void iterator(int iteratorNums,int latent,float alpha,float lambda){
		
		Matrix rating = getUseItemRating();
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
		
		int num = 0;
		while(num++<iteratorNums){
			System.out.println("共迭代"+iteratorNums+"，次正在进行第："+num+" 次迭代");
			for (int u = 0; u < rating.getRowDimension(); u++) {
				for (int i = 0; i < rating.getColumnDimension(); i++) {
					for (int k = 0; k < latent; k++) {
						double error = rating.getArray()[u][i]-times(uk[u],kiMatrix.getMatrix(0, kiMatrix.getRowDimension()-1, i, i).transpose().getArray()[0]);
						uk[u][k]=uk[u][k]+alpha*error*ki[k][i]-lambda*uk[u][k];
						ki[k][i]=ki[k][i]+alpha*error*uk[u][k]-lambda*ki[k][i];
					}
				}
				
			}
		}
		
		preditRating = ukMatrix.times(kiMatrix);
	}
	public ModelBaseCF(String path,String splitSymbol){
		loadData(path, splitSymbol);
		iterator(10, 20,0.03f, 0.01f);
//		Matrix rating = getUseItemRating();
//		double[][] uk = new double[rating.getRowDimension()][latent];
//		double[][] ki = new double[latent][rating.getColumnDimension()];
//		
//		for (int i = 0; i < uk.length; i++) {
//			for (int j = 0; j < uk[i].length; j++) {
//				uk[i][j]=Math.random();
//			}
//		}
//		for (int i = 0; i < ki.length; i++) {
//			for (int j = 0; j < ki[i].length; j++) {
//				ki[i][j]=Math.random();
//			}
//		}
//		Matrix ukMatrix = new Matrix(uk);
//		Matrix kiMatrix = new Matrix(ki);
//		
//		int num = 0;
//		while(num++<iteratorNums){
//			System.out.println("正在进行第："+num+" 次迭代");
//			for (int u = 0; u < rating.getRowDimension(); u++) {
//				for (int i = 0; i < rating.getColumnDimension(); i++) {
//					for (int k = 0; k < this.latent; k++) {
//						double error = rating.getArray()[u][i]-times(uk[u],kiMatrix.getMatrix(0, kiMatrix.getRowDimension()-1, i, i).transpose().getArray()[0]);
//						uk[u][k]=uk[u][k]+alpha*error*ki[k][i]-lambda*uk[u][k];
//						ki[k][i]=ki[k][i]+alpha*error*uk[u][k]-lambda*ki[k][i];
//					}
//				}
//				
//			}
//		}
//		
//		preditRating = ukMatrix.times(kiMatrix);
	}
	/**
	 * 
	 * @param path
	 * @param splitSymbol
	 * @param iteratorNums 迭代次数
	 * @param latent 隐类个数
	 * @param alpha 
	 * @param lambda
	 */
	public ModelBaseCF(String path,String splitSymbol,int iteratorNums,int latent,float alpha,float lambda){
		loadData(path, splitSymbol);
		iterator(iteratorNums,latent, iteratorNums, lambda);
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
	public double ratingPredictWithId(int userId,int itemId){
		if(this.userIndex.get(userId)==null){
			System.out.println("用户："+userId+"不存在");
			return 0;
		}
		if(this.itemIndex.get(itemId)==null){
			System.out.println("物品："+itemId+"不存在");
		    return 0;
		}
		return ratingPredict(this.userIndex.get(userId),this.itemIndex.get(itemId));
	}
	public double ratingPredict(int userIndex,int itemIndex){
		return preditRating.get(userIndex, itemIndex);
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
