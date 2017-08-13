package com.wugk.kmtc;

import net.sf.json.JSONObject;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		JSONObject json = new JSONObject();
		json.put("a", 1);
		json.put("b", 2);
		json.put("c", 3);
		System.out.println(json.getString("b"));
	}

}
