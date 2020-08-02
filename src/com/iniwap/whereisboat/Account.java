package com.iniwap.whereisboat;

public class Account {
	public String name;
	public String password;
	public int type;
	public double longitude;
	public double latitude;
	//this is lastlocation
	public int location_id;
	
	public boolean is_online;
	
	public Account(String name,String password,int type){
		this.name = name;
		this.password = password;
		this.type = type;

	}
}
