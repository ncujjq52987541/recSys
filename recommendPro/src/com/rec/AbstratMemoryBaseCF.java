package com.rec;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import Jama.Matrix;

public abstract class AbstratMemoryBaseCF {
	/**
	 * 物品id--索引号
	 * itemIndex.put(1, 0);
	 * itemIndex.put(2, 1);
	 * itemIndex.put(3, 2);
	 * itemIndex.put(4, 3);
	 */
	protected TreeMap<Integer,Integer> itemIndex  = new TreeMap<Integer,Integer>();
	/**
	 * 索引号--物品id
	 * indexItem.put(0, 1);
	 * indexItem.put(1, 2);
	 * indexItem.put(2, 3);
	 * indexItem.put(3, 4);
	 */
	protected TreeMap<Integer,Integer> indexItem  = new TreeMap<Integer,Integer>();
	protected TreeMap<Integer,Integer> userIndex  = new TreeMap<Integer,Integer>();
	protected TreeMap<Integer,Integer> indexUser  = new TreeMap<Integer,Integer>();
	/**
	 * 用户数量
	 */
	protected int userNumber;
	/**
	 * 物品数量
	 */
	protected int itemNumber;
	protected int neighborNumber=50;//相似邻居个数
	/**
	 * 用户评分矩阵
	 */
	protected Matrix useItemRating;
	/**
	 * 用户评分填充矩阵，即空值按一定策略填充
	 */
//	protected Matrix userItemRatingWithFill;
//	
//	public Matrix fillUserItemRating(Matrix userItemRating){
//		userItemRatingWithFill = userItemRating.copy();
//		for (int i = 0; i < userItemRatingWithFill.getRowDimension(); i++) {
//			for (int j = 0; j < userItemRatingWithFill.getColumnDimension(); j++) {
//				if(userItemRatingWithFill.get(i, j)==0){
//					double[] itemRatings = userItemRating.getMatrix(0, userItemRating.getRowDimension()-1, j, j).transpose().getArray()[0];
//					double itemAvg = MathUtil.getAverageIngoreZero(itemRatings);//所有用户对一物品评分平均分
////					userItemRatingWithFill.set(i, j, MathUtil.getAverageIngoreZero(itemRatings));
//					
//					double[] userRatings=userItemRating.getMatrix(i, i, 0, userItemRating.getColumnDimension()-1).getArray()[0];
//					double userAvg = MathUtil.getAverageIngoreZero(userRatings);//某一用户对物品评分的平均分
////					userItemRatingWithFill.set(i, j, MathUtil.getAverageIngoreZero(userRatings));
//					double w = 0.5;
//					userItemRatingWithFill.set(i, j, itemAvg*w+userAvg*(1-w));
//				}
//				
//			}
//		}
//		return userItemRatingWithFill;
//	}
	
	/**
	 * pearson相关系数计算
	 * p(x,y) = (∑xy-∑x∑y/N) / sqrt((∑x2-(∑x)2/N)*(∑y2-(∑y)2/N))
	 * @param x 向量
	 * @param y 向量
	 * 计算时实际分6部分，分别是
	 * 1、∑xy
	 * 2、∑x∑y  具体计算时∑x和∑y分别计算再相乘
	 * 3、∑x2
	 * 4、(∑x)2  具体计算时是∑x*∑x
	 * 5、∑y2
	 * 6、(∑y)2 具体计算时是∑y*∑y
	 * 最后再进行汇总，2是平方项(写在注释中不明显)
	 * 参与计算的x,y向量中同位置都不为0的数才进行计算，比如
	 * x向量是：{0,3.5,5.0,3.5,0,3.0,5.0} 
	 * y向量是：{0,3.0,3.5,0,2.0,2.0,0}
	 * 则需对输入向量进行处理，处理后的x和y分别是
	 * x={3.5,5.0,3.0}  y={3.0,3.5,2.0}
	 * N的是处理过后向量的大小
	 * @return
	 */
	public double pearson(double[] x,double[] y){
		List<Double> x1 = new ArrayList<Double>();
		List<Double> y1 = new ArrayList<Double>();
		for (int i = 0; i < y.length; i++) {
//			if(x[i]!=0&&y[i]!=0){
				x1.add(x[i]);
				y1.add(y[i]);
//			}
		}

		int n = x1.size();
		if(n==0) return 0;
		double part1=0.0f,part2_1=0.0f,part2_2=0.0f,part3=0.0f,part5=0.0f;
		for (int i = 0; i < n; i++) {
			double xvalue = x1.get(i);
			double yvalue = y1.get(i);
			part1 += xvalue*yvalue;
			part2_1 += xvalue;
			part2_2 += yvalue;
			part3 += xvalue*xvalue;
			part5 += yvalue*yvalue;
		}
		double numerator = (part1-part2_1*part2_2/n);//分子
		double denominator = Math.sqrt((part3-part2_1*part2_1/n)*(part5-part2_2*part2_2/n));//分母
		if(denominator==0){
			return 0;
		}
		double result = numerator/denominator;
		return result;
	}
	/**
	 * 余弦相似度计算
	 * @param p1
	 * @param p2
	 * @return
	 */
  public static double cosine(double[] p1, double[] p2) {
	    double dotProduct = 0.0;
	    double lengthSquaredp1 = 0.0;
	    double lengthSquaredp2 = 0.0;
	    for (int i = 0; i < p1.length; i++) {
//	    if(p1[i]!=0&&p2[i]!=0){
	      lengthSquaredp1 += p1[i] * p1[i];
	      lengthSquaredp2 += p2[i] * p2[i];
	      dotProduct += p1[i] * p2[i];
//	    }
	    }
	    double denominator = Math.sqrt(lengthSquaredp1) * Math.sqrt(lengthSquaredp2);
	    // correct for floating-point rounding errors
	    if (denominator < dotProduct) {
	      denominator = dotProduct;
	    }
	    // correct for zero-vector corner case
	    if (denominator == 0 && dotProduct == 0) {
	      return 0;
	    }
	    return dotProduct / denominator;
	  }
	/**
	 * 调整后的余弦相似度计算 为基于物品的协同过滤使用
	 * @param x
	   @param y
	   @param avgUserRating 数组的值代表某一用户对所有物品评分的平均值
	 * @return
	 */
	public double adjustedCosineForItemBased(double[] x,double[] y,double[] avgUserRating){
		double numerator = 0;
		double denominator =0;
		double denominator_1 = 0;
		double denominator_2 = 0;
		for (int i = 0; i < x.length; i++) {
			numerator += (x[i]-avgUserRating[i])*(y[i]-avgUserRating[i]);
			denominator_1 += (x[i]-avgUserRating[i])*(x[i]-avgUserRating[i]);
			denominator_2 += (y[i]-avgUserRating[i])*(y[i]-avgUserRating[i]);
		}
		denominator = Math.sqrt(denominator_1)*Math.sqrt(denominator_2);
		if(denominator==0){
			return 0;
		}
		return numerator/denominator;
	}
	/**
	 * 调整的余弦相似度 为基于用户的协同过滤使用
	 * @param x
	 * @param y
	 * @return
	 */
	public double adjustedCosineForUserBased(double[] x,double[] y){
		double numerator = 0;
		double denominator =0;
		double denominator_1 = 0;
		double denominator_2 = 0;
		double avgX = 0;
		double avgY = 0;
		for (int i = 0; i < y.length; i++) {
			avgX+=x[i];
			avgY+=y[i];
		}
		avgX = avgX/x.length;
		avgY = avgY/x.length;
		for (int i = 0; i < x.length; i++) {
			numerator += (x[i]-avgX)*(y[i]-avgY);
			denominator_1 += (x[i]-avgX)*(x[i]-avgX);
			denominator_2 += (y[i]-avgY)*(y[i]-avgY);
		}
		denominator = Math.sqrt(denominator_1)*Math.sqrt(denominator_2);
		if(denominator==0){
			return 0;
		}
		return numerator/denominator;
	}
	/**
	 * 欧式距离
	 * @param x
	 * @param y
	 * @return
	 */
	public double euclidean(double[] x,double[] y){
//		double xmin = MathUtil.getMin(x);
//		double xmax = MathUtil.getMax(x);
//		double ymin = MathUtil.getMin(y);
//		double ymax = MathUtil.getMax(y);
//		for (int i = 0; i < y.length; i++) {
//			x[i]=(x[i]-xmin)/(xmax-xmin);
//			y[i]=(y[i]-ymin)/(ymax-ymin);
//		}
		double xavg = MathUtil.getAverage(x);
		double xstandard = MathUtil.getStandardDiviation(x);
		double yavg = MathUtil.getAverage(y);
		double ystandard = MathUtil.getStandardDiviation(y);
		for (int i = 0; i < y.length; i++) {
			x[i]=(x[i]-xavg)/xstandard;
			y[i]=(y[i]-yavg)/ystandard;
		}	
		double sum = 0;
		for (int i = 0; i < y.length; i++) {
			sum+=(x[i]-y[i])*(x[i]-y[i]);
		}
		double r = Math.sqrt(sum);
		if(r==0){
			return 1;
		}
		return 1/(1+r);
	}
	/**
	 * tanimoto系数
	 */
	public double tanimoto(double[] x,double[] y){
		double part1=0,part2=0,part3=0;
		for (int i = 0; i < y.length; i++) {
			part1+=x[i]+y[i];
			part2+=x[i]*x[i];
			part3+=y[i]*y[i];
		}
		double denominator = (Math.sqrt(part2)+Math.sqrt(part3)-part1);
		if(denominator==0){//当分母为0时到底如何处理，还想再查资料，目前看返回0是最好的
			return 0;
		}
		return part1/denominator;
		
	}
	public void printResult(List<Result> r){
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			System.out.println("用户="+result.getUserId()+",itemId="+result.getItemId()+",相似度="+result.getSimDegree());
		}
	}
	/**
	 * 加载数据
	 * @param path
	 * @param splitSymbol
	 */
	protected void loadData(String path,String splitSymbol){
    	FileInputStream fis =null;
		try {
			fis = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	BufferedInputStream bis = new BufferedInputStream(fis);
    	DataInputStream dis = new DataInputStream(bis);
    	
    	String str = null;
    	
		TreeSet<Integer> items = new TreeSet<Integer>();
		TreeSet<Integer> users = new TreeSet<Integer>();
		
		try {
//			dis.mark(dis.available());
			while((str=dis.readLine())!=null){
				String[] datas = str.split(splitSymbol);
				int userId = Integer.parseInt(datas[0]);
				int itemId = Integer.parseInt(datas[1]);
				items.add(itemId);
				users.add(userId);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.userNumber = users.size();
		this.itemNumber = items.size();
		double[][] rating = new double[users.size()][items.size()];
//		double[][] itemSimilar = new double[items.size()][items.size()];
		int index = 0;
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Integer integer = (Integer) iterator.next();
			itemIndex.put(integer, index);
			indexItem.put(index, integer);
			index++;
		}
		index=0;
		for (Iterator iterator = users.iterator(); iterator.hasNext();) {
			Integer integer = (Integer) iterator.next();
			userIndex.put(integer, index);
			indexUser.put(index, integer);
			index++;
		}
		//再读文件，初始化用户评分矩阵
		try {
			fis = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	bis = new BufferedInputStream(fis);
    	dis = new DataInputStream(bis);
		try {
			while((str=dis.readLine())!=null){
				String[] datas = str.split(splitSymbol);
				int userId = Integer.parseInt(datas[0]);
				int itemId = Integer.parseInt(datas[1]);
				rating[userIndex.get(userId)][itemIndex.get(itemId)] = Float.parseFloat(datas[2]);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			dis.close();
			bis.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.useItemRating = new Matrix(rating);
//		this.itemSimiliar = new Matrix(itemSimilar);
	}


	/**
	 * @param userIndex 用户索引值
	 * 获取用户未评过分的物品索引值集合
	 * @return
	 */
	public List<Integer> getUserUnRating(int userIndex){
		List<Integer> l = new ArrayList<Integer>();
		double[] rating = useItemRating.getMatrix(userIndex, userIndex,0,useItemRating.getColumnDimension()-1).getArray()[0];
		for (int i = 0; i < rating.length; i++) {
			if(rating[i]==0){
				l.add(i);
			}
		}
		return l;
	}
	

	/**
	 * 必须在子类中实现，如果是基于用户的协同过滤则初始化用户相似度矩阵
	 * 			         如果是基于物品的协同过滤则初始化物品相似度矩阵
	 */
	public abstract void initUserOrItemSimMatrix();
	public abstract void simMatrixCompute(String similiar);
	//测试用后可删除该方法
	public Matrix getUseItemRating() {
		return useItemRating;
	}
	public int getUserNumber() {
		return userNumber;
	}
	public int getItemNumber() {
		return itemNumber;
	}
}
