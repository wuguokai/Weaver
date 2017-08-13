package weaver.interfaces.workflow.action;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import weaver.conn.RecordSet;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

//出差流程根据出差日期和到职号回写HR系统补卡档数据
public class IntoTravelTime extends BaseBean implements Action {

	BaseBean basebean = new BaseBean();
	@Override
	public String execute(RequestInfo requestInfo) {
		
		RecordSet rs = new RecordSet();
		RecordSetDataSource ds = new RecordSetDataSource("HR");
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");	
		String requestId=Util.null2String(requestInfo.getRequestid());//获取流程请求ID
		basebean.writeLog("==========出差流程回写开始！");
		try {
			String sql = "select bs_numb,bs_start_date,bs_end_date from formtable_main_54 where requestid='"+requestId+"' ";
			rs.executeSql(sql);
			basebean.writeLog("==========查询出差人员及时间： "+sql);
			String pemn = null;
			String startDate = null;
			String endDate = null;
			while(rs.next()){
				pemn = rs.getString(1);
				startDate = rs.getString(2);
				endDate = rs.getString(3);
				basebean.writeLog("=========pemn:startDate:endDate = "+pemn+" : "+startDate+" : "+endDate);
			}
			
			for(String date = startDate;dayDiff(date,endDate)>=0;date=addDate(date,1)){//根据日期循环	
			String sql1 = "select (case when shiftid is null then '-1' else shiftid end ),(case when JBID is null then '-1' else JBID end )" +
					" from hrqw_dutydata " +
					" where pemn="+pemn+" and dutydate='"+date+"'";
			ds1.executeSql(sql1);
			basebean.writeLog("===========查询对应日期的班别和假别： "+sql1);
			String shiftid = null;
			String JBID = null;
			while(ds1.next()){
				shiftid = ds1.getString(1);
				JBID = ds1.getString(2);
				basebean.writeLog("=========== shiftid : JBID = "+shiftid+" : "+JBID);
			}
			
			String sql2 = "select starttime,endtime from HRQW_SHIFT where shiftid='"+shiftid+"'";
			ds2.executeSql(sql2);
			basebean.writeLog("===============查询班别对应的正常上下班时间： "+sql2);
			String starttime = null;
			String endtime = null;
			while(ds2.next()){
				starttime = ds2.getString(1);
				endtime = ds2.getString(2);
			}
			
			//取出 的是08:00:00和17:00:00，需要转换成0800和1700
			if(!"".equals(starttime)){
				starttime = starttime.substring(0, 2)+starttime.substring(3, 5);	
			}
			if(!"".equals(endtime)){
				endtime = endtime.substring(0, 2)+endtime.substring(3,5);
			}
			
			if(JBID.equals("-1") || JBID.equals("D")){
				sql = "update hrqw_dutydata set ondutydate='"+starttime+"',offdutydate='"+endtime+"' where pemn='"+pemn+"' and dutydate='"+date+"' ";
				ds.executeSql(sql);
				basebean.writeLog("===========出差时间在正常上班时候插入打卡记录： "+sql);
			}
			
			}
			
		} catch (Exception e) {
			basebean.writeLog("==========出差流程回写错误信息："+e);
		}
		
		return Action.SUCCESS;
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
	
		//计算日期差值
				public int dayDiff(String startDate, String endDate){		
					SimpleDateFormat   df   =   new   SimpleDateFormat("yyyy-MM-dd"); 
					int days = 0;
					try{
					  Date   begin=df.parse(startDate);   
					  Date   end   =   df.parse(endDate);
					
					  double  between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
					  days=(int) (between/(24*3600));   	  
					}catch (Exception e)   
					{   
					}
					basebean.writeLog("===================days："+ days);
					return days;	  
				}
		
}
