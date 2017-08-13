package com.wugk.kmtc;

import net.sf.json.JSONObject;
import weaver.general.BaseBean;
import weaver.interfaces.workflow.action.IntoLeaveTimeAction;

public class TestGetHours {
	BaseBean basebean = new BaseBean();

	public String testGet(String pemn ,String startDate, String startTime, String endDate, String endTime){
		
		IntoLeaveTimeAction ilta = new IntoLeaveTimeAction();
		JSONObject json = ilta.diffDay(pemn, startDate, startTime, endDate, endTime);

		for(String date = startDate;ilta.dayDiff(date,endDate)==0;ilta.addDate(date,1)){//根据日期循环
			float hours = (Float) json.get(date);	//请假时长
			basebean.writeLog("==========="+date+"这一天的请假时长： "+hours);
		}
		return "ok";		
	}
}
