package com.rec;

public class Result implements Comparable<Result>{
	private int userId;
	private int itemId;
	private double simDegree;//用来存相似度或评分
	public int getItemId() {
		return itemId;
	}
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}
	
	public double getSimDegree() {
		return simDegree;
	}
	public void setSimDegree(double simDegree) {
		this.simDegree = simDegree;
	}
	public int compareTo(Result o) {
		if(simDegree>o.getSimDegree()){
			return -1;
		}else if(simDegree<o.getSimDegree()){
			return 1;
		}else{
			return 0;
		}
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
}
