package com.wugk.kmtc;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;
 
public class LeaveDay_V1 {
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public String diffDay(String pemn ,String startDate, String startTime, String endDate, String endTime){
		
		
		
		basebean.writeLog("===================数据："+ pemn+" , "+startDate+" , "+startTime+" ,"+ endDate+" , "+endTime);
		JSONArray jsonArr = new JSONArray();
		//JSONObject json1 = new JSONObject();
		Map<Integer, String> shiftids = new HashMap<Integer, String>();
		//JSONObject json2 = new JSONObject();
		Map<Integer, String> JBIDs = new HashMap<Integer, String>();
		JSONObject json3 = new JSONObject();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		//班别
		String sql1 = "select (case when shiftid is null then '-1' else shiftid end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================sql1："+ sql1);
		//假别
		String sql2 = "select (case when JBID is null then '-1' else JBID end ) " +
				" from hrqw_dutydata " +
				" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
		
		ds2.executeSql(sql2);
		basebean.writeLog("===================sql2："+ sql2);
		
		int i = 0;
		//班别存入
		
		while(ds1.next()){	
			shiftids.put(i, ds1.getString(1));
			basebean.writeLog("===================shiftids["+i+"] = "+ shiftids.get(i));
			i++;
		}
		basebean.writeLog("===================shiftids的i = "+i);
		
		i=0;
		while(ds2.next()){
			JBIDs.put(i, ds2.getString(1));
			basebean.writeLog("===================JBIDs["+i+"] = "+ JBIDs.get(i));
			i++;
		}
		basebean.writeLog("===================JBIDs的i = "+i);
		
		int days = dayDiff(startDate,endDate);	//请假跨越的天数
		float day = 0;	//请假天数
		float hour1 = 0;//第一天请假的小时数
		float hour2 = 0;//中间天请假的小时数
		float hour3 = 0;//最后一天请假的小时数
		basebean.writeLog("===================请假跨越的天数："+ (days+1));
		
		try {
			if(days==0){//同一天，直接计算
				hour2 = 0;			
				String shiftid = shiftids.get(0);
				String JBID = JBIDs.get(0);
				basebean.writeLog("===================shiftid:JBID  "+ shiftid+":"+JBID);
				String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				String afterTime = null;		//休息结束时间
				float dutyhours = 0;		//规则对应的上班小时数
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(afterTime.equals("00:00")) afterTime = "23:59";
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhours-dutyhour;	//休息时长
				String midtime = addTime(afterTime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);				
				
				if(JBID.equals("R") || JBID.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBID);
					hour1 = 0;
					hour3 = 0;
				}else{
										
					//跨天的怎么比较，如果是00:00则转换成23:59
					if(timeCompare(starttime, startTime)<=0){	//开始时间在上班之前
						hour1 = dutyhours;	
					}else if(timeCompare(starttime, startTime)>0 && timeCompare(midtime, startTime)<=0){	//开始时间在上班和休息之间
						basebean.writeLog("===================startTime , endtime , resthour"+startTime+" , "+endtime+" , "+resthour);
						hour1 = timeDiff(startTime,endtime)+resthour;			
					}else if(timeCompare(midtime, startTime)>0 && timeCompare(afterTime, startTime)<=0){	//开始时间在休息时间
						hour1 = timeDiff(afterTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0 && timeCompare(endtime, startTime)<=0){	//开始时间休息结束时间和下班时间之间
						hour1 = timeDiff(startTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0){	//开始时间在下班之后
						hour1 = 0;	
					}
					
					if(timeCompare(starttime, endTime)<=0){	//结束时间在上班之前
						hour3 = 0;	
					}else if(timeCompare(starttime, endTime)>0 && timeCompare(midtime, endTime)<=0){	//结束时间在上班和休息之间
						hour3 = timeDiff(starttime,endTime);			
					}else if(timeCompare(midtime, endTime)>0 && timeCompare(afterTime, endTime)<=0){	//结束时间在休息时间
						hour3 = timeDiff(starttime,midtime);	
					}else if(timeCompare(afterTime, endTime)>0 && timeCompare(endtime, endTime)<=0){	//结束时间在休息结束和下班之间
						hour3 = timeDiff(starttime,endTime)+resthour;	
					}else if(timeCompare(afterTime, endTime)>0){		//结束时间在下班之后
						hour3 = dutyhours;	
					}
					basebean.writeLog("===================hour1: hour2:　hour3:  "+ hour1+":"+hour2+":"+hour3);
					hour1 = hour1+hour3-dutyhours;	//相加减去一天的时间
					hour3 = 0;	
				}
				
				float hour = hour1+hour2+hour3;
				day = hour/dutyhours;		//请假天数
				
				
			}else{	//请假日期不在同一天的
					
				//第一天时间
				String shiftid0 = shiftids.get(0);
				String JBID0 = JBIDs.get(0);
				String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid0+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				String afterTime = null;		//休息结束时间
				float dutyhours = 0;		//规则对应的上班小时数
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(afterTime.equals("00:00")) afterTime = "23:59";
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhours-dutyhour;	//休息时长
				String midtime = addTime(afterTime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);
				
				if(JBID0.equals("R") || JBID0.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBID0);
					hour1 = 0;
				}else{
						
					if(timeCompare(starttime, startTime)<=0){	//开始时间在上班之前
						hour1 = dutyhours;	
					}else if(timeCompare(starttime, startTime)>0 && timeCompare(midtime, startTime)<=0){	//开始时间在上班和休息之间
						hour1 = timeDiff(startTime,endtime)+resthour;			
					}else if(timeCompare(midtime, startTime)>0 && timeCompare(afterTime, startTime)<=0){	//开始时间在休息时间
						hour1 = timeDiff(afterTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0 && timeCompare(endtime, startTime)<=0){	//开始时间休息结束时间和下班时间之间
						hour1 = timeDiff(startTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0){	//开始时间在下班之后
						hour1 = 0;	
					}
				}
				float day1 = 0;
				day1 = hour1/dutyhours;
				 
				//最后一天时间
				String shiftidlast = shiftids.get(days);
				String JBIDlast = JBIDs.get(days);
				basebean.writeLog("===================shiftidlast：JBIDlast"+ shiftidlast+" :"+JBIDlast);
				sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftidlast+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(afterTime.equals("00:00")) afterTime = "23:59";
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				resthour = dutyhours-dutyhour;	//休息时长
				midtime = addTime(afterTime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);
										
				if(JBIDlast.equals("R") || JBIDlast.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBIDlast);
					hour3 = 0;
				}else{				
					
					if(timeCompare(starttime, endTime)<=0){	//结束时间在上班之前
						hour3 = 0;	
					}else if(timeCompare(starttime, endTime)>0 && timeCompare(midtime, endTime)<=0){	//结束时间在上班和休息之间
						hour3 = timeDiff(starttime,endTime);			
					}else if(timeCompare(midtime, endTime)>0 && timeCompare(afterTime, endTime)<=0){	//结束时间在休息时间
						hour3 = timeDiff(starttime,midtime);	
					}else if(timeCompare(afterTime, endTime)>0 && timeCompare(endtime, endTime)<=0){	//结束时间在休息结束和下班之间
						hour3 = timeDiff(starttime,endTime)+resthour;	
					}else if(timeCompare(afterTime, endTime)>0){		//结束时间在下班之后
						hour3 = dutyhours;	
					}			
				}
				float day3 = 0;
				day3 = hour3/dutyhours;
				
				float day2 = 0;
				//中间时间
				if(days>=2){
				for(int j = 1; j<days; j++){
					
					float hour = 0;
					String shiftid = shiftids.get(j);
					String JBID = JBIDs.get(j);
					basebean.writeLog("===================shiftids["+j+"]"+ shiftid+"=====JBIDs["+j+"]"+ JBID);
					sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid+"'";
					ds3.executeSql(sql3);			
					basebean.writeLog("===================sql3："+ sql3);				
					while(ds3.next()){					
						dutyhours = ds3.getFloat(4);
					}
					
					if(JBID.equals("R") || JBID.equals("T")){
						basebean.writeLog("=============假日： "+JBID);
						hour = 0;
					}else{										
						
						hour = dutyhours;
					}
					
					day2 = day2+hour/dutyhours;
					hour2 = hour2+hour;		
				}
			}
				basebean.writeLog("===================hour1: hour2:　hour3:"+ hour1+":"+hour2+":"+hour3);
				day = day1+day2+day3;
			}
			float hourTotal = hour1+hour2+hour3;	//请假时长
			float dayTotal = day;	//请假时长
			BigDecimal l=new BigDecimal(Double.toString(dayTotal));
			dayTotal=(float) l.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
			json3.put("hourTotal", hourTotal);
			json3.put("dayTotal", dayTotal);
			json3.put("error", 1);
		} catch (NullPointerException npe){
			basebean.writeLog("===================异常： "+npe);
			json3.put("error", 0);
			basebean.writeLog("===================日期对应的班别或者假别没数据");			
		}catch (Exception e){
			basebean.writeLog("=======异常： "+e);
			e.printStackTrace();			
		}	
			
		jsonArr.add(json3);
		return jsonArr.toString();
	
	}
	
	//计算时间差值
	public float timeDiff(String startTime, String endTime){
		basebean.writeLog("===================startTime , endTime  "+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("HH:mm"); 
		float hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  System.out.println("===================begin:  end"+ begin+" : "+end);
		  System.out.println("===================begin:  end.gettime"+ begin.getTime()+" : "+end.getTime());
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  System.out.println("===================between："+ between);
//		  int   day=between/(24*3600);   
		  hour=between/3600;   
//		  int   minute=between%3600/60;   
//		  int   second=between%60; 
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================hour："+ hour);
		System.out.println("===================hour："+ hour);
		if(hour>=0){
		  return hour;
		}else{
			return 24+hour;
		}		 		  
	}
	
	//时间比较
	public int timeCompare(String startTime, String endTime){	
		basebean.writeLog("===================startTime , endTime"+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("HH:mm"); 
		int   minute = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  //System.out.println("===================begin:  end"+ begin+" : "+end);
		  //System.out.println("===================begin:  end.gettime"+ begin.getTime()+" : "+end.getTime());
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  //System.out.println("===================between："+ between);
//		  int   day=between/(24*3600);   
		  //hour=between%(24*3600)/3600;   
		  minute=(int) (between/60);   
//		  int   second=between%60; 
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================minute："+ minute);
		//System.out.println("===================hour："+ hour);
		
		  return minute;		  
	}
	
	//计算日期差值
	public int dayDiff(String startDate, String endDate){		
		SimpleDateFormat   df   =   new   SimpleDateFormat("yyyy-MM-dd"); 
		int days = 0;
		try{
		  Date   begin=df.parse(startDate);   
		  Date   end   =   df.parse(endDate);
		
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  days=(int) (between/(24*3600));   
		  //hour=between%(24*3600)/3600;   
//		  int   minute=between%3600/60;   
//		  int   second=between%60; 
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================days："+ days);
		return days;	  
	} 
	
	//时间加上diff小时数
	public String addTime(String time, float diff ){
		String value = null;
		int diffmin = (int) (diff*60);	//小时转换成分钟
		try{
			//System.out.println(date);
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");//格式化
			Date inDate = sdf.parse(time);
			Calendar calendar = Calendar.getInstance();// 输入时间
			calendar.setTime(inDate);
			calendar.add(Calendar.MINUTE, diffmin);//加diffmin分钟
			
			Date outDate = calendar.getTime();
			value = sdf.format(outDate);
			//System.out.println(sdf.format(outDate));
		}catch (Exception e) {
		}
		basebean.writeLog("===================value："+ value);
		return value;
	}
}
