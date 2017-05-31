package com.rec;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TestItemBaseCF {
	private String splitSymbol=",";
	private Set<Integer> userRecItems = new HashSet<Integer>();//给用户推荐物品总数
	private int totalItems;//系统总共物品数
	private int totalCount;//总记录条数
	private double ratingCount;
	private Map<Integer,List<UserItemRating>> adduserPre(Map<Integer,List<UserItemRating>> userItemRating,int userId,int itemId,float rating){
		List<UserItemRating> list = userItemRating.get(userId);
		UserItemRating uir = new UserItemRating();
		uir.setItemId(itemId);
		uir.setUserId(userId);
		uir.setRating(rating);
		if(list==null){
			List<UserItemRating> l = new ArrayList<UserItemRating>();
			l.add(uir);
			userItemRating.put(userId, l);
		}else{
			list.add(uir);
		}
		return userItemRating;
	}
	
	public Map<Integer,List<UserItemRating>> loadTestData(String path){
		Map<Integer,List<UserItemRating>> userItemRating = new HashMap<Integer,List<UserItemRating>>();
		FileInputStream fis =null;
		try {
			fis = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	BufferedInputStream bis = new BufferedInputStream(fis);
    	DataInputStream dis = new DataInputStream(bis);
    	
    	String str = null;
		try {
			while((str=dis.readLine())!=null){
				String[] datas = str.split(splitSymbol);
				int userId = Integer.parseInt(datas[0]);
				int itemId = Integer.parseInt(datas[1]);
				float rating = Float.parseFloat(datas[2]);
				totalCount++;
				ratingCount+=rating;
				adduserPre(userItemRating,userId,itemId,rating);
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
		return userItemRating;
	}
	public int intersection(List<Result> r,List<UserItemRating> t){
		Set<Integer> finalResult = new HashSet<Integer>();
		Set<Integer> rSet = new HashSet<Integer>();
		Set<Integer> tSet = new HashSet<Integer>();
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			rSet.add(result.getItemId());
		}
		
		for (Iterator iterator = t.iterator(); iterator.hasNext();) {
			UserItemRating uir = (UserItemRating) iterator.next();
			tSet.add(uir.getItemId());
		}
		
		finalResult.addAll(rSet);
		finalResult.retainAll(tSet);
		
		return finalResult.size();
	}
	public int union(Set userRecItems , List<Result> r){
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			userRecItems.add(result.getItemId());
		}
		return userRecItems.size();
	}
	public void addToSet(Set<Integer> s,List<Result> r){
		for (Iterator iterator = r.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			s.add(result.getItemId());
		}
	}
	public void recommendTest(int recommendNum){
		String  testData = "D:\\tmp\\recommend\\netflix_test\\part-00000";
		String  trainData = "D:\\tmp\\recommend\\netflix_train\\part-00000";
//		String  testData = "D:\\tmp\\recommend\\aa_test\\part-00000";
//		String  trainData = "D:\\tmp\\recommend\\aa_train\\part-00000";
//		String trainData = "D:\\tmp\\recommend\\ml-100k\\u1.base";
//		String testData = "D:\\tmp\\recommend\\ml-100k\\u1.test";
		Map<Integer,List<UserItemRating>> userItemRating = loadTestData(testData);
//		ModelBaseCF sm = new ModelBaseCF(trainData,splitSymbol);
//		ModelBaseCF sm = new ModelBaseCF(trainData,splitSymbol,2,100,0.02f,0.01f);
		ItemBasedCF sm = new ItemBasedCF(trainData,splitSymbol,SimiliarType.COSINE);
//		UserBasedCF sm = new UserBasedCF(trainData,splitSymbol,SimiliarType.TANIMOTO);
		
		int numerator_rt = 0;//精确度分子
		int denominator_r = 0;//精确度分母
		int denominator_t = 0;//召回率分母
		float f1 = 0;//调和平均数
		Set<Integer> numerator_r = new HashSet<Integer>();//覆盖率分子
		totalItems = sm.getItemNumber();
		Set<Integer> keys = userItemRating.keySet();
		int totalUsers = keys.size();
		int i=1;
		float precision = 0f;
		float recall = 0f;
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			Integer key = (Integer) iterator.next();
			List<Result> result = sm.recommend(key,recommendNum);
			numerator_rt+=intersection(result,userItemRating.get(key));
			denominator_r+=result.size();
			denominator_t+=userItemRating.get(key).size();
			addToSet(numerator_r,result);
			
			precision = (float)numerator_rt/denominator_r;
			recall = (float)numerator_rt/denominator_t;
			f1=(2*precision*recall)/(recall+precision);
			System.out.println("总共："+totalUsers+"用户，已算完："+i+++"个"+
			",当前准确率："+precision+
			",当前召回率："+recall+
			",F1:"+f1+
			",当前覆盖率："+((float)numerator_r.size())/totalItems);
		}
		precision = (float)numerator_rt/denominator_r;
		recall =(float)numerator_rt/denominator_t;
		f1=(2*precision*recall)/(recall+precision);
		System.out.println("准确率："+precision+
				",召回率："+recall+
				",F1:"+f1+
				",覆盖率："+((float)numerator_r.size())/totalItems);
	}
	public void recommendMAEAndRMSE(){
		String testData = "D:\\tmp\\recommend\\aa_test\\part-00000";
		String  trainData= "D:\\tmp\\recommend\\aa_train\\part-00000";
//		String trainData = "D:\\tmp\\recommend\\ml-100k\\u1.base";
//		String testData = "D:\\tmp\\recommend\\ml-100k\\u1.test";
		Map<Integer,List<UserItemRating>> userItemRating = loadTestData(testData);
//		ModelBaseCF sm = new ModelBaseCF(trainData,splitSymbol);
//		ModelBaseCF sm = new ModelBaseCF(trainData,splitSymbol,2,100,0.02f,0.01f);
		ItemBasedCF sm = new ItemBasedCF(trainData,splitSymbol,SimiliarType.COSINE);
//		UserBasedCF sm = new UserBasedCF(trainData,splitSymbol,SimiliarType.COSINE);
//		sm.getItemSimiliar().print(2, 2);
		float sum=0;
		int index=0;
		Collection<List<UserItemRating>> c = userItemRating.values();
		for (Iterator iterator = c.iterator(); iterator.hasNext();) {
			List<UserItemRating> list = (List<UserItemRating>) iterator.next();
			for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
				UserItemRating userItemRating2 = (UserItemRating) iterator2
						.next();
				int userId = userItemRating2.getUserId();
				int itemId = userItemRating2.getItemId();
				double rating = userItemRating2.getRating();
				double preRating = sm.ratingPredictWithId(userId, itemId);
				sum+=Math.abs(rating-preRating);
				index++;
				System.out.println("userId="+userId+"ItemId="+itemId+",预测评分="+preRating+",实际评分="+rating+"当前MAE="+sum/index);
			}
		}
		
		System.out.println(index+"最终MAE="+sum/index);
	}
	public static void test1(){
		ItemBasedCF sm = new ItemBasedCF("D:\\tmp\\recommend\\ml-100k\\u1.base","\t");
		List<Result> results = sm.recommend(1, 20);
		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
			Result result = (Result) iterator.next();
			System.out.println("itemId"+result.getItemId()+",rating"+result.getSimDegree());
		}
	}
	public static void main(String[] args) throws IOException {
		long begin = System.currentTimeMillis();
		TestItemBaseCF test = new TestItemBaseCF();
		test.recommendTest(20);
//		test.recommendMAEAndRMSE();
		long end = System.currentTimeMillis();
		System.out.println("");
		System.out.println("运行时间："+(end-begin)/1000.0);
		//123
//		test1();
	}
}
