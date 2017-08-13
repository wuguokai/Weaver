package com.wugk.kmtc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;
 
public class CheckSubmit_V1 {
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public String check(String pemn ,String startDate, String startTime, String endDate, String endTime){				
		
		basebean.writeLog("===================数据："+ pemn+" , "+startDate+" , "+startTime+" ,"+ endDate+" , "+endTime);
		JSONArray jsonArr = new JSONArray();
		JSONObject json1 = new JSONObject();
		JSONObject json2 = new JSONObject();
		JSONObject json3 = new JSONObject();
		Map<Integer, String> shiftids = new HashMap<Integer, String>();
		Map<Integer, String> JBIDs = new HashMap<Integer, String>();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		boolean check = true;	//是否允许提交
		String error = "1";
		String starttime = null;	//规则上班开始时间
		String endtime = null;		//规则下班时间
		String afterTime = null;		//休息结束时间
		float dutyhours = 0;		//规则对应的上班小时数
		String ondutydate = null;
		String offdutydate = null;
		
		try {
			
		
		//开始到结束日期间班别和假别
		String sql1 = "select (case when shiftid is null then '-1' else shiftid end ),(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================sql1："+ sql1);
	
		int i = 0;
		while(ds1.next()){	
			shiftids.put(i, ds1.getString(1));
			JBIDs.put(i, ds1.getString(2));			
			basebean.writeLog("===================开始日期对应的班别和假别： "+ shiftids.get(i)+" : "+JBIDs.get(i));
			if(!JBIDs.get(i).equals("-1") && !JBIDs.get(i).equals("D") && !JBIDs.get(i).equals("Z")){
				check = false;
			}
			i++;
		}

		if(i>1) i=i-1;
		else i=0;
		basebean.writeLog("=================== i = "+i);
				
		//开始日期判断
		if(shiftids.get(0).equals("8") && timeCompare("08:00",startTime)<0){	//今天是8班别并且请假开始时间在8点前
			String startDate1 = addDate(startDate,-1);
			
			String sql3 = "select (case when shiftid is null then '-1' else shiftid end ), ondutydate,offdutydate,(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate = '"+startDate1+"'";
			
			ds3.executeSql(sql3);		
			basebean.writeLog("===================sql3："+ sql3);
			
			while(ds3.next()){	
				json1.put("shiftidl", ds3.getString(1));
				ondutydate = ds3.getString(2);
				offdutydate = ds3.getString(3);
				json1.put("JBIDl", ds3.getString(4));
				basebean.writeLog("===================开始日期对应的班别和打卡时间： "+ json1.get("shiftidl")+" :　"+ondutydate+" : "+offdutydate);
			}
			
			if(json1.get("shiftidl").equals("8")){
				if(!json1.get("JBIDl").equals("-1") && !json1.get("JBIDl").equals("D") && !json1.get("JBIDl").equals("Z")){
					check = false;
				}else if(!"".equals(ondutydate) && "".equals(offdutydate)){
					check = false;
				}else if("".equals(ondutydate) && !"".equals(offdutydate)){
					check = false;
				}
			}
			
		}else{	
			//班别信息			
			String sql2 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftids.get(0)+"'";
			
			ds2.executeSql(sql2);
			basebean.writeLog("===================sql2："+ sql2);
		
			while(ds2.next()){
				starttime = ds2.getString(1);
				endtime = ds2.getString(2);
				afterTime = ds2.getString(3);	//对应的是数据库表中的midtime
				dutyhours = ds3.getFloat(4);
				basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);
			}
			
			//打卡时间信息
			String sql3 = "select (case when shiftid is null then '-1' else shiftid end ), ondutydate,offdutydate,(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate = '"+startDate+"'";
			
			ds3.executeSql(sql3);		
			basebean.writeLog("===================sql3："+ sql3);
			
			while(ds3.next()){	
				json1.put("shiftid", ds3.getString(1));
				ondutydate = ds3.getString(2);
				offdutydate = ds3.getString(3);
				json1.put("JBID", ds3.getString(4));
				basebean.writeLog("===================开始日期对应的班别和打卡时间： "+ json1.get("shiftidl")+" :　"+ondutydate+" : "+offdutydate);
			}
		
		basebean.writeLog("=================== "+!"".equals(ondutydate) +" : "+ "".equals(offdutydate) +" : "+ "".equals(ondutydate) +" : "+ !"".equals(offdutydate));
		basebean.writeLog("=================== "+((!"".equals(ondutydate) && "".equals(offdutydate)) || ("".equals(ondutydate) && !"".equals(offdutydate))));
		if((!"".equals(ondutydate) && "".equals(offdutydate)) || ("".equals(ondutydate) && !"".equals(offdutydate))){
			//一个卡没打
			if(timeCompare(starttime,startTime)>0){
				check = false;
			}
		}		
	}
		
		
		//结束日期判断
		
		if(shiftids.get(i).equals("8") && timeCompare("20:00",endTime)<0){	//今天是8班别并且请假开始时间在8点前
			String endDate1 = addDate(endDate,-1);
			
			String sql3 = "select (case when shiftid is null then '-1' else shiftid end ), ondutydate,offdutydate,(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate = '"+endDate1+"'";
			
			ds3.executeSql(sql3);		
			basebean.writeLog("===================sql3："+ sql3);
			
			while(ds3.next()){	
				json1.put("shiftidl", ds3.getString(1));
				ondutydate = ds3.getString(2);
				offdutydate = ds3.getString(3);
				json1.put("JBID", ds3.getString(4));
				basebean.writeLog("===================开始日期对应的班别和打卡时间： "+ json1.get("shiftidl")+" :　"+ondutydate+" : "+offdutydate);
			}
			
			if(json1.get("shiftidl").equals("8") && timeCompare("08:00", endTime)<0){
				if(!json1.get("JBIDl").equals("-1") && !json1.get("JBIDl").equals("D") && !json1.get("JBIDl").equals("Z")){
					check = false;
				}else if(!"".equals(ondutydate) && "".equals(offdutydate)){
					check = false;
				}else if("".equals(ondutydate) && !"".equals(offdutydate)){
					check = false;
				}
			}
			
		}else{	//
			//班别信息			
			String sql2 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftids.get(i)+"'";
			
			ds2.executeSql(sql2);
			basebean.writeLog("===================sql2："+ sql2);
		
			while(ds2.next()){
				starttime = ds2.getString(1);
				endtime = ds2.getString(2);
				afterTime = ds2.getString(3);	//对应的是数据库表中的midtime
				dutyhours = ds2.getFloat(4);
				basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);
			}
			
			//打卡时间信息
			String sql3 = "select (case when shiftid is null then '-1' else shiftid end ), ondutydate,offdutydate,(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate = '"+startDate+"'";
			
			ds3.executeSql(sql3);		
			basebean.writeLog("===================sql3："+ sql3);
			
			while(ds3.next()){	
				json1.put("shiftid", ds3.getString(1));
				ondutydate = ds3.getString(2);
				offdutydate = ds3.getString(3);
				json1.put("JBID", ds3.getString(4));
				basebean.writeLog("===================开始日期对应的班别和打卡时间： "+ json1.get("shiftidl")+" :　"+ondutydate+" : "+offdutydate);
			}
		
		if((!"".equals(ondutydate) && "".equals(offdutydate)) || ("".equals(ondutydate) && !"".equals(offdutydate))){
			//一个卡没打
			if(timeCompare(starttime,startTime)>0){
				check = false;
			}
		}		
	}
		} catch (Exception e) {
			error = "0";
			basebean.writeLog("==============异常信息 ： "+e);
		}		
				
		json3.put("check", check);
		json3.put("error", error);
		jsonArr.add(json3);
		return jsonArr.toString();
	
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
