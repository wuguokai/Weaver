package com.wugk.kmtc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;

public class TestLeaveDay {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
	}
	
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public void diffDay(String pemn ,String startDate, String startTime, String endDate, String endTime){
		
		JSONArray jsonArr = new JSONArray();
		JSONObject json1 = new JSONObject();
		JSONObject json2 = new JSONObject();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		//班别
		String sql1 = "select (case when shiftid is null then '-1' else shiftid end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between "+startDate+" and "+endDate+"";
		//假别
		String sql2 = "select (case when JBID is null then '-1' else shiftid end ) " +
				" from hrqw_dutydata " +
				" where pemn="+pemn+" and dutydate between "+startDate+" and "+endDate+"";
		if (!ds1.executeSql(sql1)) {
			basebean.writeLog("===================sql1："+ sql1);
		}
		if (!ds2.executeSql(sql2)) {
			basebean.writeLog("===================sql2："+ sql2);
		}
		
		int i = 0;
		//班别存入
		while(ds1.next()){	
			json1.put("shiftid"+i, ds1.getString(1));
			i++;
		}
		
		while(ds2.next()){
			json2.put("JBID"+i, ds2.getString(1));
			i++;
		}
		float days = dayDiff(startDate,endDate);	//请假跨越的天数
		float hour1 = 0;//第一天请假的小时数
		float hour2 = 0;//中间天请假的小时数
		float hour3 = 0;//最后一天请假的小时数
		
		
		if(days==0){//同一天，直接计算
			hour2 = 0;			
			String shiftid = json1.getString("shiftid0");
			String JBID = json2.getString("JBID0");
			String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid="+shiftid+"";
			if (!ds3.executeSql(sql3)) {
				basebean.writeLog("===================sql3："+ sql3);
			}			
			if(JBID.equals("R") || JBID.equals("T")){	//假日就等于O
				hour1 = 0;
				hour3 = 0;
			}else{
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				String midtime = null;		//休息开始时间
				float dutyhours = 0;		//规则对应的上班小时数
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);
					dutyhours = ds3.getFloat(4);
				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhour-dutyhours;	//休息时长
				String afterTime = addTime(midtime, resthour);	//休息结束时间
					
				if(startTime.compareTo(starttime)<=0){	//开始时间在上班之前
					hour1 = dutyhours;	
				}else if(startTime.compareTo(starttime)>0 && startTime.compareTo(midtime)<=0){	//开始时间在上班和休息之间
					hour1 = timeDiff(startTime,endtime)-resthour;			
				}else if(startTime.compareTo(midtime)>0 && startTime.compareTo(afterTime)<=0){	//开始时间在休息时间
					hour1 = timeDiff(afterTime,endtime);	
				}else if(startTime.compareTo(afterTime)>0 && startTime.compareTo(endtime)<=0){	//开始时间休息结束时间和下班时间之间
					hour1 = timeDiff(startTime,endtime);	
				}else if(startTime.compareTo(endtime)>0){	//开始时间在下班之后
					hour1 = 0;	
				}
				
				if(endTime.compareTo(starttime)<=0){	//结束时间在上班之前
					hour3 = 0;	
				}else if(endTime.compareTo(starttime)>0 && endTime.compareTo(midtime)<=0){	//结束时间在上班和休息之间
					hour3 = timeDiff(starttime,endTime);			
				}else if(endTime.compareTo(midtime)>0 && endTime.compareTo(afterTime)<=0){	//结束时间在休息时间
					hour3 = timeDiff(starttime,midtime);	
				}else if(endTime.compareTo(afterTime)>0 && endTime.compareTo(endtime)<=0){	//结束时间在休息结束和下班之间
					hour3 = timeDiff(starttime,endTime)-resthour;	
				}else if(endTime.compareTo(endtime)>0){		//结束时间在下班之后
					hour3 = dutyhours;	
				}
				hour1 = hour1+hour3-dutyhours;	//相加减去一天的时间
				hour3 = 0;	
			}
			
		}else{	//请假日期不在同一天的
				
			//第一天时间
			String shiftid0 = json1.getString("shiftid0");
			String JBID0 = json2.getString("JBID0");
			String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid="+shiftid0+"";
			if (!ds3.executeSql(sql3)) {
				basebean.writeLog("===================sql3："+ sql3);
			}			
			if(JBID0.equals("R") || JBID0.equals("T")){	//假日就等于O
				hour1 = 0;
			}else{
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				String midtime = null;		//休息开始时间
				float dutyhours = 0;		//规则对应的上班小时数
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);
					dutyhours = ds3.getFloat(4);
				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhour-dutyhours;	//休息时长
				String afterTime = addTime(midtime, resthour);	//休息结束时间
					
				if(startTime.compareTo(starttime)<=0){	//开始时间在上班之前
					hour1 = dutyhours;	
				}else if(startTime.compareTo(starttime)>0 && startTime.compareTo(midtime)<=0){	//开始时间在上班和休息之间
					hour1 = timeDiff(startTime,endtime)-resthour;			
				}else if(startTime.compareTo(midtime)>0 && startTime.compareTo(afterTime)<=0){	//开始时间在休息时间
					hour1 = timeDiff(afterTime,endtime);	
				}else if(startTime.compareTo(afterTime)>0 && startTime.compareTo(endtime)<=0){	//开始时间休息结束时间和下班时间之间
					hour1 = timeDiff(startTime,endtime);	
				}else if(startTime.compareTo(endtime)>0){	//开始时间在下班之后
					hour1 = 0;	
				}
			}
			
			//最后一天时间
			String shiftidlast = json1.getString("shiftid"+(days-1));
			String JBIDlast = json2.getString("JBID"+(days-1));
			sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid="+shiftidlast+"";
			if (!ds3.executeSql(sql3)) {
				basebean.writeLog("===================sql3："+ sql3);
			}			
			if(JBIDlast.equals("R") || JBIDlast.equals("T")){	//假日就等于O
				hour3 = 0;
			}else{
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				String midtime = null;		//休息开始时间
				float dutyhours = 0;		//规则对应的上班小时数
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);
					dutyhours = ds3.getFloat(4);
				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhour-dutyhours;	//休息时长
				String afterTime = addTime(midtime, resthour);	//休息结束时间
				
				if(endTime.compareTo(starttime)<=0){	//结束时间在上班之前
					hour3 = 0;	
				}else if(endTime.compareTo(starttime)>0 && endTime.compareTo(midtime)<=0){	//结束时间在上班和休息之间
					hour3 = timeDiff(starttime,endTime);			
				}else if(endTime.compareTo(midtime)>0 && endTime.compareTo(afterTime)<=0){	//结束时间在休息时间
					hour3 = timeDiff(starttime,midtime);	
				}else if(endTime.compareTo(afterTime)>0 && endTime.compareTo(endtime)<=0){	//结束时间在休息结束和下班之间
					hour3 = timeDiff(starttime,endTime)-resthour;	
				}else if(endTime.compareTo(endtime)>0){		//结束时间在下班之后
					hour3 = dutyhours;	
				}				
			}
			
			//中间时间
			for(int j = 1; j<json1.size()-1; j++){
				
				float hour = 0;
				String shiftid = json1.getString("shiftid"+j);
				String JBID = json2.getString("JBID"+j);
				sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid="+shiftid+"";
				if (!ds3.executeSql(sql3)) {
					basebean.writeLog("===================sql3："+ sql3);
				}
				
				if(JBID.equals("R") || JBID.equals("T")){
					hour = 0;
				}else{
					String starttime = null;
					String endtime = null;
					String midtime = null;
					float dutyhours = 0;
					while(ds3.next()){
						starttime = ds3.getString(1);
						endtime = ds3.getString(2);
						midtime = ds3.getString(3);
						dutyhours = ds3.getFloat(4);
					}
					
					//float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班时长
					//float resthour = dutyhour-dutyhours;	//休息时长
					//String afterTime = addTime(midtime, resthour);
					
					hour = dutyhours;
				}
				
				hour2 = hour2+hour;		
			}		
		}
		
		float hourTotal = hour1+hour2+hour3;	//请假时长
		
		
	}
	
	//计算时间差值
	public float timeDiff(String startTime, String endTime){	
		SimpleDateFormat   df   =   new   SimpleDateFormat("HH:mm:ss"); 
		float hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
//		  int   day=between/(24*3600);   
		  hour=between%(24*3600)/3600;   
//		  int   minute=between%3600/60;   
//		  int   second=between%60; 
		}catch (Exception e)   
		{   
		}
		if(hour>=0){
		  return hour;
		}else{
			return 24+hour;
		}
		 
		  
	}
	
	//计算日期差值
	public float dayDiff(String startDate, String endDate){		
		SimpleDateFormat   df   =   new   SimpleDateFormat("yyyy-MM-dd"); 
		float days = 0;
		try{
		  Date   begin=df.parse(startDate);   
		  Date   end   =   df.parse(endDate);
		
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  days=between/(24*3600);   
		  //hour=between%(24*3600)/3600;   
//		  int   minute=between%3600/60;   
//		  int   second=between%60; 
		}catch (Exception e)   
		{   
		}
		  return days;
	  
	}
	
	//时间加上diff小时数
	public String addTime(String time, float diff ){
		String value = null;
		int diffmin = (int) (diff*60);	//小时转换成分钟
		try{
			//System.out.println(date);
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");//格式化
			Date inDate = sdf.parse(time);
			Calendar calendar = Calendar.getInstance();// 输入时间
			calendar.setTime(inDate);
			calendar.add(Calendar.MINUTE, diffmin);//加diffmin分钟
			
			Date outDate = calendar.getTime();
			value = sdf.format(outDate);
			//System.out.println(sdf.format(outDate));
		}catch (Exception e) {
		}
		return value;
	}

}
