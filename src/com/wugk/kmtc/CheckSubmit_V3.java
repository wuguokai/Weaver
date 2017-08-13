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
 
public class CheckSubmit_V3 {
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public String check(String pemn ,String startDate, String startTime, String endDate, String endTime){				
		basebean.writeLog("===================请假提交验证开始");
		basebean.writeLog("===================数据："+ pemn+" , "+startDate+" , "+startTime+" ,"+ endDate+" , "+endTime);
		JSONArray jsonArr = new JSONArray();
		JSONObject json1 = new JSONObject();
//		JSONObject json2 = new JSONObject();
		JSONObject json3 = new JSONObject();
		Map<Integer, String> shiftids = new HashMap<Integer, String>();
		Map<Integer, String> JBIDs = new HashMap<Integer, String>();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
//		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		boolean check = true;	//是否允许提交
		String error = "1";
//		String starttime = null;	//规则上班开始时间
//		String endtime = null;		//规则下班时间
//		String afterTime = null;		//休息结束时间
//		float dutyhours = 0;		//规则对应的上班小时数
		String ondutydate = null;
		String offdutydate = null;
		
		try {
			//提交不能跨班别
			String sql1 = "select count(distinct shiftid) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================提交不能跨班别sql1："+ sql1);
			
		while(ds1.next()){	
			int num = ds1.getInt(1);
			if(num>1){
				check = false;
			}
		}	
		
		//开始到结束日期间班别和假别
		sql1 = "select (case when shiftid is null then '-1' else shiftid end ),(case when JBID is null then '-1' else JBID end ) " +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================sql1："+ sql1);
	
		int i = 0;
		while(ds1.next()){	
			shiftids.put(i, ds1.getString(1));
			JBIDs.put(i, ds1.getString(2));			
			basebean.writeLog("===================日期对应的班别和假别： "+ shiftids.get(i)+" : "+JBIDs.get(i));
			if(!JBIDs.get(i).equals("-1") && !JBIDs.get(i).equals("D") && !JBIDs.get(i).equals("Z") && !JBIDs.get(i).equals("R") && !JBIDs.get(i).equals("T")){
				check = false;
			}
			i++;
		}

		if(i>1) i=i-1;
		else i=0;
		basebean.writeLog("=================== i = "+i);
				
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
		String sysDate = df.format(new Date());// new Date()为获取当前系统时间
		basebean.writeLog("==========当前日期: "+sysDate);
		
		if(dayDiff(startDate, sysDate)>0){//开始日期在当前日期之前
			for(String date = startDate;dayDiff(date,sysDate)>0;date=addDate(date,1)){//根据日期循环	
				
				//打卡时间信息
				String sql3 = "select (case when shiftid is null then '-1' else shiftid end ), ondutydate,offdutydate,(case when JBID is null then '-1' else JBID end ) " +
						" from hrqw_dutydata " +
						" where pemn="+pemn+" and dutydate = '"+date+"'";
				
				ds3.executeSql(sql3);		
				basebean.writeLog("===================获取打卡时间sql3："+ sql3);
				
				while(ds3.next()){	
					json1.put("shiftid", ds3.getString(1));
					ondutydate = ds3.getString(2);
					//当为空的时候会报错
					if(!"".equals(ondutydate)){
					ondutydate = ondutydate.substring(0, 2)+":"+ondutydate.substring(2, 4);
					}
					offdutydate = ds3.getString(3);
					if(!"".equals(offdutydate)){
					offdutydate = offdutydate.substring(0, 2)+":"+offdutydate.substring(2, 4);
					}
					json1.put("JBID", ds3.getString(4));
					basebean.writeLog("===================日期"+date+"对应的班别和打卡时间：  "+ json1.get("shiftid")+" :　"+ondutydate+" : "+offdutydate);
				}
			
//			basebean.writeLog("=================== "+!"".equals(ondutydate) +" : "+ "".equals(offdutydate) +" : "+ "".equals(ondutydate) +" : "+ !"".equals(offdutydate));
//			basebean.writeLog("=================== "+((!"".equals(ondutydate) && "".equals(offdutydate)) || ("".equals(ondutydate) && !"".equals(offdutydate))));
			if("".equals(ondutydate) || "".equals(offdutydate)){
				//打卡记录为空
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
