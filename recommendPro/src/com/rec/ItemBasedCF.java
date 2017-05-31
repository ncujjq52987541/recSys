package com.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import Jama.Matrix;

public class ItemBasedCF extends AbstratMemoryBaseCF{
	
	/**
	 * 物品相似度矩阵
	 */
	private Matrix itemSimiliar;
	private double[] itemAvgRating;//缓存每个物品的平均评分
	public ItemBasedCF(String path,String splitSymbol){
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
	public ItemBasedCF(String path,String splitSymbol,String similiarType){
		loadData(path, splitSymbol);
		initUserOrItemSimMatrix();
		simMatrixCompute(similiarType);
		
	}
	@Override
	public void initUserOrItemSimMatrix(){
		double[][] itemSimilar = new double[itemIndex.size()][itemIndex.size()];
		this.itemSimiliar = new Matrix(itemSimilar);
	}

	/**
	 * 获取用户对每个物品的平均评分
	 * @param useItemRating
	 * @return
	 */
	public double[] getUserAvgRating(Matrix userItemRating){
		double[] result = new double[useItemRating.getRowDimension()];
		double[][] rating = userItemRating.getArray();
		double sum = 0;
		int index = 0;
		for (int i = 0; i < rating.length; i++) {
			for (int j = 0; j < rating[i].length; j++) {
//				if(rating[i][j]!=0){
					sum +=rating[i][j];
					index++;
//				}
			}
			result[i] = sum/index;
			sum = 0;
			index= 0;
		}
		return result;
	}
	/**
	 * 计算生成物品相似度矩阵
	 */
	@Override
	public void simMatrixCompute(String similiar){
		System.out.println("正计算相似度计算矩阵...");
		itemAvgRating = this.itemRatingAvgCache();
//		fillUserItemRating(useItemRating);
//		useItemRating.print(2, 2);
//		userItemRatingWithFill.print(2, 2);
		double[][] sim = itemSimiliar.getArray();
		double[] userAvgRating = null;
		if(SimiliarType.ADJUSTEDCOSINE.equals(similiar)){//只有ADJUSTEDCOSINE才需计算这个值
			userAvgRating = getUserAvgRating(useItemRating);
		}
		for (int i = 0; i < sim.length; i++) {
			System.out.println("正在计算第"+i+"行");
			for (int j = 0; j < sim[i].length; j++) {
				if(i<j){
					double[][] imatrix = useItemRating.getMatrix(0, useItemRating.getRowDimension()-1, i, i).transpose().getArray();
					double[][] jmatrix = useItemRating.getMatrix(0, useItemRating.getRowDimension()-1, j, j).transpose().getArray();
					switch(similiar){
						case SimiliarType.PEARSON:sim[i][j] = pearson(imatrix[0], jmatrix[0]);break;
						case SimiliarType.ADJUSTEDCOSINE:sim[i][j] = adjustedCosineForItemBased(imatrix[0], jmatrix[0],userAvgRating);break;
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
		
		System.out.println("相似度矩阵计算完成...");
	}
	/**
	 * 物品评分平均值
	 * @return
	 */
	public double[] itemRatingAvgCache(){
		double[] avg = new double[itemSimiliar.getRowDimension()];
		for (int i = 0; i < avg.length; i++) {
			avg[i]=MathUtil.getAverage(
					useItemRating.getMatrix(0, useItemRating.getRowDimension()-1, i,i).transpose().getArray()[0]
					);
		}
		return avg;
	}
	
	
	public double itemAvgRating(Integer itemIndex){
		return itemAvgRating[itemIndex];
	}
	/**
	 * 最原始的推荐,不是严格按公式的
	 * 物品k的平均得分
	 * @return
	 */
	@Deprecated
	public List<Result> recommendOrigin(int userId,int k){
		if(userIndex.get(userId)==null){
			System.out.println("用户"+userId+"不存在,无法推荐");
			return new ArrayList<Result>();
		}
		int uindex = userIndex.get(userId);
//		itemSimiliar.print(2, 2);
//		try {
//			itemSimiliar.print(new PrintWriter(new BufferedOutputStream(new FileOutputStream("D:\\tmp\\recommend\\ml-100k\\simMatrix.txt"))), 2, 2);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Matrix result = itemSimiliar.times(useItemRating.getMatrix(uindex, uindex, 0, useItemRating.getColumnDimension()-1).transpose());
		//根据公式都除以分母
//		for(int i=0;i<result.getRowDimension();i++){
//			double s = sum(itemSimiliar.getMatrix(i, i,0,itemSimiliar.getColumnDimension()-1).getArray()[0]);
//			double newValue = result.get(i, 0)/s;
//			result.set(i, 0, newValue);
//			
//		}
		//拿到该用户的评分向量
		double[] rating = useItemRating.getMatrix(uindex, uindex, 0, useItemRating.getColumnDimension()-1).getArray()[0];
		List<Result> finalResult = new ArrayList<Result>();
		for (int i = 0; i < rating.length; i++) {
			if(rating[i]==0){//仅对未评过分的推荐
				Result r = new Result();
				r.setItemId(indexItem.get(i));
				r.setSimDegree(result.getArray()[i][0]);
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
	
	///////////////
	/**
	 * 
	 * @param itemIdex 物品索引号
	 * @return 返回的是相似物品的索引号集合！
	 */
	private Set<Integer> getItemNeighbors(int itemIdex,int nums){
		double[] items = itemSimiliar.getMatrix(itemIdex, itemIdex,0,itemSimiliar.getColumnDimension()-1).getArray()[0];
		List<Result> r = new ArrayList<Result>();//临时用这个做排序用
		for (int i = 0; i < items.length; i++) {
			Result result = new Result();
			result.setItemId(i);
			result.setSimDegree(items[i]);
			r.add(result);
		}
		Collections.sort(r);
		
		int sum=0;
		Set<Integer> ret = new HashSet<Integer>();
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			if(itemIdex!=result.getItemId()&&result.getSimDegree()>0){//把自己除去
				ret.add(result.getItemId());
				++sum;
			}
			
			if(sum>=nums) break;
		}
		return ret;
	}
	public double ratingPredictWithId(int userId,int itemId){
		if(this.userIndex.get(userId)==null){
			System.out.println("用户："+userId+"不存在");
			return itemAvgRating(itemIndex.get(itemId));
		}
		if(this.itemIndex.get(itemId)==null){
			System.out.println("物品："+itemId+"不存在");
		    return 0;
		}
		return ratingPredict(this.userIndex.get(userId),this.itemIndex.get(itemId));
	}
	/**
	 * 预测用户对物品的评分，注意传入的都是索引值
	 * @param userIndex
	 * @param itemIndex
	 * @return
	 */
	public double ratingPredict(int userIndex,int itemIndex){
//		double[] urating = useItemRating.getMatrix(userIndex, userIndex, 0, useItemRating.getColumnDimension()-1).getArray()[0];
//		double numerator =0; 
//		double denominator = 0;
//		for (int i = 0; i < urating.length; i++) {
//			if(urating[i]!=0&&itemSimiliar.get(itemIndex, i)>0){
//				numerator+=itemSimiliar.get(itemIndex, i)*(useItemRating.get(userIndex, i)-itemAvgRating(i));
//				denominator+=itemSimiliar.get(itemIndex, i);
//			}
//		}
//		if(denominator==0){
//			return itemAvgRating(itemIndex);
//		}
//		return itemAvgRating(itemIndex)+numerator/denominator;
		
		
		Set<Integer> items = getItemNeighbors(itemIndex,this.neighborNumber);
		double numerator =0; 
		double denominator = 0;
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Integer k = (Integer) iterator.next();
			numerator+=itemSimiliar.get(itemIndex, k)*(useItemRating.get(userIndex, k)-itemAvgRating(k));
			denominator+=itemSimiliar.get(itemIndex, k);
		}
		if(denominator==0){
			return itemAvgRating(itemIndex);
		}
		return itemAvgRating(itemIndex)+numerator/denominator;
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
	//////////////
	public ItemBasedCF() {
		super();
	}
	
	public Matrix getItemSimiliar() {
		return itemSimiliar;
	}
	public static void main(String[] args) {
		ItemBasedCF t = new ItemBasedCF("D:\\tmp\\recommend\\test\\train4.txt",",",SimiliarType.COSINE);
//		Matrix m = t.getUseItemRating();
		Matrix m = t.getItemSimiliar();
		m.print(2, 2);
//		System.out.println(t.ratingPredictWithId(1, 3));
//		m.getMatrix(1, 1, 0, m.getColumnDimension()-1).print(2, 2);
//		System.out.println(t.pearson(new double[]{0,3.5,5.0,3.5,0,3.0,5.0}, new double[]{0,3.0,3.5,0,2.0,2.0,0}));
//		t.recommend1(1, 1);
//		List<Result> r = t.recommend(1, 10);
		List<Result> r1 = t.recommend(2, 10);
//		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
//			Result result = (Result) iterator.next();
//			System.out.println(result.getItemId()+","+result.getSimDegree());
//		}
		System.out.println("");
		for (Iterator iterator = r1.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			System.out.println(result.getItemId()+","+result.getSimDegree());
		}
	}
}
