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
 
public class LeaveDay_V5 {
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public String diffDay(String pemn ,String startDate, String startTime, String endDate, String endTime){				
		basebean.writeLog("===================请假时间计算开始");
		basebean.writeLog("===================请假时间计算数据："+ pemn+" , "+startDate+" , "+startTime+" ,"+ endDate+" , "+endTime);
		JSONArray jsonArr = new JSONArray();			
		Map<Integer, String> shiftids = new HashMap<Integer, String>();
		Map<Integer, String> JBIDs = new HashMap<Integer, String>();
		JSONObject json3 = new JSONObject();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		//班别
		String sql1 = "select (case when shiftid is null then '-1' else shiftid end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between '"+addDate(startDate,-1)+"' and '"+endDate+"'";
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================sql1："+ sql1);
		//假别
		String sql2 = "select (case when JBID is null then '-1' else JBID end ) " +
				" from hrqw_dutydata " +
				" where pemn="+pemn+" and dutydate between '"+addDate(startDate,-1)+"' and '"+endDate+"'";
		
		ds2.executeSql(sql2);
		basebean.writeLog("===================sql2："+ sql2);
		
		int i = -1;
		//班别存入
		
		while(ds1.next()){	
			shiftids.put(i, ds1.getString(1));
			basebean.writeLog("===================shiftids["+i+"] = "+ shiftids.get(i));
			i++;
		}
		basebean.writeLog("===================shiftids的i = "+i);
		
		i=-1;
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
			String starttime1 = null;	//请假开始时间8班别规则上班开始时间
			String endtime1 = null;		//请假开始时间8班别规则下班时间
			String starttime2 = null;	//请假结束时间8班别规则上班开始时间
			String endtime2 = null;		//请假结束时间8班别规则下班时间
			String starttime = null;	//规则上班开始时间
			String endtime = null;		//规则下班时间
			String midtime = null;		//休息开始时间
			String afterTime = null;		//休息结束时间		
			float dutyhours = 0;		//规则对应的上班小时数
			
			if(days==0){//同一天，直接计算
				hour2 = 0;			
				String shiftidl = shiftids.get(-1);
				String JBIDl = JBIDs.get(-1);
				String shiftid = shiftids.get(0);
				String JBID = JBIDs.get(0);
				basebean.writeLog("===================shiftid:JBID  "+ shiftid+":"+JBID);
				String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);	//对应的是数据库表中的midtime
//这个是原来的需求处理方式	afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================midtime:"+ midtime+"===================dutyhours:"+ dutyhours);

				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhour-dutyhours;	//休息时长
				afterTime = addTime(midtime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ afterTime);				
				
				if (shiftid.equals("8")) {//8班别的上班时间在20:00到第二天的08:00
					
					String startTime1 = startDate+" "+startTime;	//请假开始日期和时间
					if(timeCompare("12:00", startTime)<0){		//请假开始时间对应规则开始以及结束日期和时间
						starttime1 = addDate(startDate, -1)+" "+starttime;
						endtime1 = startDate+" "+endtime;
					}else{
						starttime1 = startDate+" "+starttime;
						endtime1 = addDate(startDate, 1)+" "+endtime;
					}
					
					String endTime1 = endDate+" "+endTime;		//请假结束日期和时间
					if(timeCompare("12:00", endTime)<0){		//请假结束时间对应规则开始以及结束日期和时间
						starttime2 = addDate(endDate, -1)+" "+starttime;
						endtime2 = endDate+" "+endtime;
					}else{
						starttime2 = endDate+" "+starttime;
						endtime2 = addDate(endDate, 1)+" "+endtime;
					}										
					
					if(JBID.equals("R") || JBID.equals("T")){	//如果是假日
						basebean.writeLog("=============假日： "+JBID);
						if(timeCompare("08:00", endTime)<0){	//结束时间在8点前
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = 0;
									hour3 = 0;
								}else{
									hour1 = timeDiff(startTime, endTime);
									hour3 = 0;
								}
							}else{
								hour1 = 0;
								hour3 = 0;
							}
							
						}else if(timeCompare("08:00", startTime)<0 && timeCompare("08:00", endTime)>=0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = 0;
									hour3 = 0;
								}else{
									hour1 = timeDiff(startTime, endtime);
									hour3 = 0;
								}
							}else{
								hour1 = 0;
								hour3 = 0;
							}
							
						}else{//今天是假日   if(timeCompare("08:00", startTime)>=0)
							
							hour1 = 0;
							hour3 = 0;
						}
					}else{
						if(timeCompare("08:00", startTime)<0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = dutyhours;
								}else{
									hour1 = timeDiff(startTime, endtime)+dutyhours;
								}
							}else{
								hour1 = dutyhours;
							}
						}else if(timeCompare("08:00", startTime)>=0 && timeCompare("20:00", startTime)<0){
							hour1 = dutyhours;
						}else{
							hour1 = datetimeDiff(startTime1, endtime1);
						}
						
						if(timeCompare("20:00", endTime)<0){
									hour3 = 0;
						}else{
							hour3 = datetimeDiff(starttime2, endTime1);
						}
						basebean.writeLog("===================hour1: hour2:　hour3:  "+ hour1+":"+hour2+":"+hour3);
						hour1 = hour1+hour3-dutyhours;	//相加减去一天的时间
						hour3 = 0;
					}															
						
				}else if(JBID.equals("R") || JBID.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBID);
					hour1 = 0;
					hour3 = 0;
				}else{
										
					//跨天的怎么比较，如果是00:00则转换成23:59
					if(timeCompare(starttime, startTime)<=0){	//开始时间在上班之前
						hour1 = dutyhours;	
					}else if(timeCompare(starttime, startTime)>0 && timeCompare(midtime, startTime)<=0){	//开始时间在上班和休息之间
						basebean.writeLog("===================startTime , endtime , resthour"+startTime+" , "+endtime+" , "+resthour);
						hour1 = timeDiff(startTime,endtime)-resthour;			
					}else if(timeCompare(midtime, startTime)>0 && timeCompare(afterTime, startTime)<=0){	//开始时间在休息时间
						hour1 = timeDiff(afterTime,endtime);
					}else if(timeCompare(afterTime, startTime)>0 && timeCompare(endtime, startTime)<=0){	//开始时间休息结束时间和下班时间之间
						hour1 = timeDiff(startTime,endtime);	
					}else if(timeCompare(endTime, startTime)>0){	//开始时间在下班之后
						hour1 = 0;	
					}
					
					if(timeCompare(starttime, endTime)<=0){	//结束时间在上班之前
						hour3 = 0;	
					}else if(timeCompare(starttime, endTime)>0 && timeCompare(midtime, endTime)<=0){	//结束时间在上班和休息之间
						hour3 = timeDiff(starttime,endTime);			
					}else if(timeCompare(midtime, endTime)>0 && timeCompare(afterTime, endTime)<=0){	//结束时间在休息时间
						hour3 = timeDiff(starttime,midtime);	
					}else if(timeCompare(afterTime, endTime)>0 && timeCompare(endtime, endTime)<=0){	//结束时间在休息结束和下班之间
						hour3 = timeDiff(starttime,endTime)-resthour;	
					}else if(timeCompare(endtime, endTime)>0){		//结束时间在下班之后
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
				String shiftidl = shiftids.get(-1);
				String JBIDl = JBIDs.get(-1);
				String shiftid0 = shiftids.get(0);
				String JBID0 = JBIDs.get(0);
				String sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid0+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
								
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);	//对应的是数据库表中的midtime
//旧需求					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(midtime.equals("00:00")) midtime = "23:59";
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================midtime:"+ midtime+"===================dutyhours:"+ dutyhours);

				}				
				float dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				float resthour = dutyhour-dutyhours;	//休息时长
				afterTime = addTime(midtime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);
				
				if (shiftid0.equals("8")) {//8班别的上班时间在20:00到第二天的08:00
					
					String startTime1 = startDate+" "+startTime;	//请假开始日期和时间
					if(timeCompare("12:00", startTime)<0){		//请假开始时间对应规则开始以及结束日期和时间
						starttime1 = addDate(startDate, -1)+" "+starttime;
						endtime1 = startDate+" "+endtime;
					}else{
						starttime1 = startDate+" "+starttime;
						endtime1 = addDate(startDate, 1)+" "+endtime;
					}
										
					if(JBID0.equals("R") || JBID0.equals("T")){	//如果是假日
						basebean.writeLog("=============假日： "+JBID0);
						if(timeCompare("08:00", startTime)<0){	//开始时间在8点前
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = 0;
								}else{
									hour1 = timeDiff(startTime, endtime);
								}
							}else{
								hour1 = 0;
							}
						}else{
							hour1 = 0;
						}
					}else{
						if(timeCompare("08:00", startTime)<0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = dutyhours;
								}else{
									hour1 = timeDiff(startTime, endtime)+dutyhours;
								}
							}else{
								hour1 = dutyhours;
							}
						}else if(timeCompare("20:00", startTime)<0){
							hour1 = dutyhours;
						}else{
							hour1 = datetimeDiff(startTime1, endtime1);
						}					
					}															
				}else if(JBID0.equals("R") || JBID0.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBID0);
					hour1 = 0;
				}else{
						
					if(timeCompare(starttime, startTime)<=0){	//开始时间在上班之前
						hour1 = dutyhours;	
					}else if(timeCompare(starttime, startTime)>0 && timeCompare(midtime, startTime)<=0){	//开始时间在上班和休息之间
						hour1 = timeDiff(startTime,endtime)-resthour;			
					}else if(timeCompare(midtime, startTime)>0 && timeCompare(afterTime, startTime)<=0){	//开始时间在休息时间
						hour1 = timeDiff(afterTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0 && timeCompare(endtime, startTime)<=0){	//开始时间休息结束时间和下班时间之间
						hour1 = timeDiff(startTime,endtime);	
					}else if(timeCompare(endtime, startTime)>0){	//开始时间在下班之后
						hour1 = 0;	
					}
				}
				float day1 = 0;
				day1 = hour1/dutyhours;
				 
				//最后一天时间
				String shiftidlast = shiftids.get(days);
				String JBIDlast = JBIDs.get(days);
				JBIDl = JBIDs.get(days-1);
				shiftidl = shiftids.get(days-1);
				basebean.writeLog("===================shiftidlast：JBIDlast"+ shiftidlast+" :"+JBIDlast);
				sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftidlast+"'";
				ds3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				while(ds3.next()){
					starttime = ds3.getString(1);
					endtime = ds3.getString(2);
					midtime = ds3.getString(3);	//对应的是数据库表中的midtime
//旧需求				afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(midtime.equals("00:00")) midtime = "23:59";
					dutyhours = ds3.getFloat(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================midtime:"+ midtime+"===================dutyhours:"+ dutyhours);

				}				
				dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				resthour = dutyhour-dutyhours;	//休息时长
				afterTime = addTime(midtime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);
										
				if (shiftidlast.equals("8")) {//8班别的上班时间在20:00到第二天的08:00
					
					String endTime1 = endDate+" "+endTime;		//请假结束日期和时间
					if(timeCompare("12:00", endTime)<0){		//请假结束时间对应规则开始以及结束日期和时间
						starttime2 = addDate(endDate, -1)+" "+starttime;
						endtime2 = endDate+" "+endtime;
					}else{
						starttime2 = endDate+" "+starttime;
						endtime2 = addDate(endDate, 1)+" "+endtime;
					}										
					
					if(JBIDlast.equals("R") || JBIDlast.equals("T")){	//如果是假日
						basebean.writeLog("=============假日： "+JBID0);
						if(timeCompare("08:00", endTime)<0){	//开始时间在8点前
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour3 = 0;
								}else{
									hour3 = datetimeDiff(starttime2,endTime1);	
								}
							}else{
								hour3 = 0;
							}
						}else{
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour3 = 0;
								}else{
									hour3 = dutyhours;
								}
							}else{
								hour3 = 0;
							}
						}
					}else{
						if(timeCompare("08:00", endTime)<0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour3 = 0;
								}else{
									hour3 = datetimeDiff(starttime2, endTime1);
								}
							}else{
								hour3 = 0;
							}
						}else if(timeCompare("20:00", endTime)<0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour3 = 0;
								}else{
									hour3 = dutyhours;
								}
							}else{
								hour3 = 0;
							}
						}else{
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour3 = timeDiff(starttime, endTime);
								}else{
									hour3 = timeDiff(starttime, endTime)+dutyhours;
								}
							}else{
								hour3 = timeDiff(starttime, endTime);
							}
						}
					}	
					hour3=hour3-dutyhours;
				}else if(JBIDlast.equals("R") || JBIDlast.equals("T")){	//假日就等于O
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
						hour3 = timeDiff(starttime,endTime)-resthour;	
					}else if(timeCompare(endtime, endTime)>0){		//结束时间在下班之后
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
			basebean.writeLog("==========dayTotal : "+dayTotal);
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
	
	//计算日期时间差值
	public float datetimeDiff(String startTime, String endTime){
		basebean.writeLog("===================startTime , endTime  "+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("yyyy-MM-dd HH:mm"); 
		float hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  System.out.println("===================begin:  end"+ begin+" : "+end);
		  System.out.println("===================begin:  end.gettime"+ begin.getTime()+" : "+end.getTime());
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  System.out.println("===================between："+ between);
		  hour=between/3600;   
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================hour："+ hour);
		
		return hour;
	}
	
	//计算时间差值
	public float timeDiff(String startTime, String endTime){
		basebean.writeLog("===================startTime , endTime  "+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("HH:mm"); 
		float hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  hour=between/3600;   
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================hour："+ hour);
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
		  float   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  minute=(int) (between/60);   
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================minute："+ minute);
		
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
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");//格式化
			Date inDate = sdf.parse(time);
			Calendar calendar = Calendar.getInstance();// 输入时间
			calendar.setTime(inDate);
			calendar.add(Calendar.MINUTE, diffmin);//加diffmin分钟
			
			Date outDate = calendar.getTime();
			value = sdf.format(outDate);
		}catch (Exception e) {
		}
		basebean.writeLog("===================value："+ value);
		return value;
	}
	 
	//日期加上天数
	public String addDate(String date, int diff ){
		String value = null;
		try{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//格式化
			Date inDate = sdf.parse(date);
			Calendar calendar = Calendar.getInstance();// 输入时间
			calendar.setTime(inDate);
			calendar.add(Calendar.DATE, diff);//加diff天
			
			Date outDate = calendar.getTime();
			value = sdf.format(outDate);
		}catch (Exception e) {
		}
		basebean.writeLog("===================value："+ value);
		return value;
	}
}
