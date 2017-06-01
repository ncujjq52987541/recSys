package com.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import Jama.Matrix;

public class UserBasedCF extends AbstratMemoryBaseCF{
	/**
	 * 用户相似度矩阵
	 */
	private Matrix userSimilar;
	private double[] userAvgRating;//缓存每个用户的平均评分
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
		userAvgRating = this.userRatingAvgCache();
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
	/**
	 * 最原始的推荐,不是严格按公式的
	 * @param userId
	 * @param k
	 * @return
	 */
	@Deprecated
	public List<Result> recommendOrigin(int userId,int k){
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
	//////////////
	/**
	 * 
	 * @param userIndex 用户索引号
	 * @return 返回的是相似用户的索引号集合！
	 */
	private Set<Integer> getUserNeighbors(int userIndex,int nums){
		double[] users = userSimilar.getMatrix(userIndex, userIndex,0,userSimilar.getColumnDimension()-1).getArray()[0];
		List<Result> r = new ArrayList<Result>();//临时用这个做排序用
		for (int i = 0; i < users.length; i++) {
			Result result = new Result();
			result.setItemId(i);//现在这个实际是userId的索引号
			result.setSimDegree(users[i]);
			r.add(result);
		}
		Collections.sort(r);
		
		int sum=0;
		Set<Integer> ret = new HashSet<Integer>();
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			if(userIndex!=result.getItemId()){//把自己除去
				ret.add(result.getItemId());
				++sum;
			}
			
			if(sum>=nums) break;
		}
		return ret;
	}
	/**
	 * 预测用户对物品的评分，注意传入的都是索引值
	 * @param userIndex
	 * @param itemIndex
	 * @return
	 */
	public double ratingPredict(int userIndex,int itemIndex){
		Set<Integer> users = getUserNeighbors(userIndex,this.neighborNumber);
		double numerator =0; 
		double denominator = 0;
		for (Iterator iterator = users.iterator(); iterator.hasNext();) {
			Integer ui = (Integer) iterator.next();
			if(useItemRating.get(ui, itemIndex)>0){
			numerator+=userSimilar.get(userIndex, ui)*(useItemRating.get(ui, itemIndex)-userAvgRating(ui));
			denominator+=userSimilar.get(userIndex, ui);
			}
		}
		if(denominator==0){
			return userAvgRating(userIndex);
		}
		return userAvgRating(userIndex)+numerator/denominator;
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
	/**
	 * 获取用户对每个物品的平均评分
	 * @param useItemRating
	 * @return
	 */
	public double[] userRatingAvgCache(){
		double[] avg = new double[useItemRating.getRowDimension()];
		for (int i = 0; i < avg.length; i++) {
			avg[i]=MathUtil.getAverageIngoreZero(
					useItemRating.getMatrix(i,i, 0,useItemRating.getColumnDimension()-1).getArray()[0]
					);
		}
		return avg;
		
		
//		double[] userAvgRating = new double[useItemRating.getRowDimension()];
//		double[][] rating = useItemRating.getArray();
//		double sum = 0;
//		int index = 0;
//		for (int i = 0; i < rating.length; i++) {
//			for (int j = 0; j < rating[i].length; j++) {
////				if(rating[i][j]!=0){
//					sum +=rating[i][j];
//					index++;
////				}
//			}
//			userAvgRating[i] = sum/index;
//			sum = 0;
//			index= 0;
//		}
//		return userAvgRating;
	}
	/**
	 * 用户u的平均得分
	 * @return
	 */
	public double userAvgRating(Integer userIndex){
		return userAvgRating[userIndex];
	}
	
	/**
	 * 
	 * @param userId
	 * @param k 推荐个数
	 * @return
	 */
	public List<Result> recommend(int userId,int k){
		if(userIndex.get(userId)==null){
			System.out.println("用户"+userId+"不存在,无法推荐");
			return new ArrayList<Result>();
		}
		int uindex = userIndex.get(userId);
		
		List<Result> result = new ArrayList<Result>();
		List<Integer> unRatingItems = getUserUnRating(uindex);
		for (Iterator iterator = unRatingItems.iterator(); iterator.hasNext();) {
			Integer unRatingItem = (Integer) iterator.next();
			double rating = ratingPredict(uindex, unRatingItem);
			Result r = new Result();
			r.setUserId(userId);
			r.setItemId(this.indexItem.get(unRatingItem));
			r.setSimDegree(rating);
			result.add(r);
		}
//		printResult(result);
		Collections.sort(result);
		if(result.size()>k){
			return result.subList(0, k);
		}else{
			return result;
		}
	}
	/////////////
	@Override
	public void initUserOrItemSimMatrix() {
		double[][] userSimilar = new double[userIndex.size()][userIndex.size()];
		this.userSimilar = new Matrix(userSimilar);
		
	}
	public Matrix getUserSimilar() {
		return this.userSimilar;
	}
	public static void main(String[] args) {
		UserBasedCF t = new UserBasedCF("D:\\tmp\\recommend\\test1\\train.txt"," ",SimiliarType.COSINE);
		
//		UserBasedCF t = new UserBasedCF("D:\\tmp\\recommend\\test2\\train.txt"," ",SimiliarType.TANIMOTO);
		Matrix m = t.getUserSimilar();
		m.print(2, 2);
		List<Result> result = t.recommend(1, 10);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Result result2 = (Result) iterator.next();
			System.out.println(result2.getItemId()+","+result2.getSimDegree());
		}
	}
}
