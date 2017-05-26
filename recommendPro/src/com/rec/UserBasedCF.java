package com.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import Jama.Matrix;

public class UserBasedCF extends AbstratMemoryBaseCF{
	/**
	 * 用户相似度矩阵
	 */
	private Matrix userSimilar;
	public UserBasedCF(String path,String splitSymbol){
		loadData(path, splitSymbol);
		initUserOrItemSimMatrix();
		simMatrixCompute("");
	}
	/**
	 * 相似度计算方法
	 * @param path
	 * @param splitSymbol
	 * @param similiarType
	 */
	public UserBasedCF(String path,String splitSymbol,String similiarType){
		loadData(path, splitSymbol);
		initUserOrItemSimMatrix();
		simMatrixCompute(similiarType);
	}

	@Override
	public void simMatrixCompute(String similiar){
		double[][] sim = userSimilar.getArray();
//		double[] userAvgRating = null;
//		if(SimiliarType.ADJUSTEDCOSINE.equals(similiar)){//只有ADJUSTEDCOSINE才需计算这个值
//			userAvgRating = getUserAvgRating(useItemRating);
//		}
		for (int i = 0; i < sim.length; i++) {
			for (int j = 0; j < sim[i].length; j++) {
				if(i<j){
					double[][] imatrix = useItemRating.getMatrix(i,i,0,useItemRating.getColumnDimension()-1).getArray();
					double[][] jmatrix = useItemRating.getMatrix(j,j,0, useItemRating.getColumnDimension()-1).getArray();
					switch(similiar){
						case SimiliarType.PEARSON:sim[i][j] = pearson(imatrix[0], jmatrix[0]);break;
						case SimiliarType.ADJUSTEDCOSINE:sim[i][j] = adjustedCosineForUserBased(imatrix[0], jmatrix[0]);break;
						case SimiliarType.COSINE:sim[i][j] = cosine(imatrix[0], jmatrix[0]);break;
						case SimiliarType.EUCLIDEAN:sim[i][j] = euclidean(imatrix[0], jmatrix[0]);break;
						case SimiliarType.TANIMOTO:sim[i][j] = tanimoto(imatrix[0], jmatrix[0]);break;
						default:sim[i][j] = pearson(imatrix[0], jmatrix[0]);
					}
					
				}
			}
		}
		
		//填充下半角矩阵
		for (int i = 0; i < sim.length; i++) {
			for (int j = 0; j < sim[i].length; j++) {
				if(i>j){
					sim[i][j] = sim[j][i];
				}else if(i==j){
					sim[i][j]=1;
				}
			}
		}
	}
	public List<Result> recommend(int userId,int k){
		int uindex = userIndex.get(userId);
		Matrix userSimMatrix = userSimilar.getMatrix(uindex, uindex,0,userSimilar.getColumnDimension()-1);
		double[][] result = userSimMatrix.times(useItemRating).getArray();
		
		//拿到该用户的评分向量
		double[] rating = useItemRating.getMatrix(uindex, uindex, 0, useItemRating.getColumnDimension()-1).getArray()[0];
		List<Result> finalResult = new ArrayList<Result>();
		for (int i = 0; i < rating.length; i++) {
			if(rating[i]==0){//仅对未评过分的推荐
				Result r = new Result();
				r.setItemId(indexItem.get(i));
				r.setSimDegree(result[0][i]);
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
	@Override
	public void initUserOrItemSimMatrix() {
		double[][] userSimilar = new double[userIndex.size()][userIndex.size()];
		this.userSimilar = new Matrix(userSimilar);
		
	}
	public Matrix getUserSimilar() {
		return this.userSimilar;
	}
	public static void main(String[] args) {
		UserBasedCF t = new UserBasedCF("D:\\tmp\\recommend\\test2\\train.txt"," ",SimiliarType.TANIMOTO);
		Matrix m = t.getUserSimilar();
		m.print(2, 2);
		List<Result> result = t.recommend(1, 10);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Result result2 = (Result) iterator.next();
			System.out.println(result2.getItemId()+","+result2.getSimDegree());
		}
	}
}
