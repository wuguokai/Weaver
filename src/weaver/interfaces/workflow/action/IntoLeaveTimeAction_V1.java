package weaver.interfaces.workflow.action;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.soa.workflow.request.RequestInfo;

public class IntoLeaveTimeAction_V1 extends BaseBean implements Action {

	BaseBean basebean = new BaseBean();
	@Override
	public String execute(RequestInfo requestinfo) {
		RecordSetDataSource rs = new RecordSetDataSource("HR");
		RecordSet rs1 = new RecordSet();	//OA
		RecordSetDataSource rs2 = new RecordSetDataSource("HR");
		RecordSetDataSource rs3 = new RecordSetDataSource("HR");
		RecordSetDataSource rs4 = new RecordSetDataSource("HR");
		String requestId=Util.null2String(requestinfo.getRequestid());//获取流程请求ID
		
		try {
			String sql1 = "select pemntext,leave_begn_date, leave_begn_time, leave_end_date,leave_end_time,classify" +
						" from formtable_main_23"+
						" where requestid='"+requestId+"'";
			rs1.executeSql(sql1);
			basebean.writeLog("===================sql1："+ sql1);
			String pemn = null;
			String startDate = null;
			String startTime = null;
			String endDate = null;
			String endTime = null;
			int classify = -1 ;
			double pych = 0;	//去年剩余年休
			double pysh = 0;	//去年剩余调休
			double pmch = 0;	//剩余年休
			double pmsh = 0;	//剩余调休
			double sjlasthours = 0;	//剩余事假
			String JBIDs = null;
			while(rs1.next()){
				pemn = rs1.getString(1);
				startDate = rs1.getString(2);
				startTime = rs1.getString(3);
				endDate = rs1.getString(4);
				endTime = rs1.getString(5);
				classify = rs1.getInt(6);
			}
			 
			JSONObject json = diffDay(pemn, startDate, startTime, endDate, endTime);
			
			for(String date = addDate(startDate,-1);dayDiff(date,endDate)>=0;date=addDate(date,1)){//根据日期循环			
				double hours;
				try {
					hours = (Double) json.get(date);	//请假时长
				} catch (NullPointerException npe) {
					basebean.writeLog("=========请假时长为0:"+npe);
					hours=0;
				}			
				basebean.writeLog("==========="+date+"这一天的请假时长： "+hours);
			}
			
			if(classify == 0){
				JBIDs = "V";
			}else if(classify == 1){
				JBIDs = "T";
			}else if(classify == 2){
				JBIDs = "W";
			}else if(classify == 3){
				JBIDs = "S";
			}else if(classify == 4){
				JBIDs = "C";
			}else if(classify == 5){
				JBIDs = "change";		//免加班就换班别
			}else if(classify == 6){
				JBIDs = "U";
			}
			
			int days = dayDiff(startDate,endDate);
			basebean.writeLog("==========请假跨越的天数： "+(days+1));
			String sql = "";//回写语句
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
			String sysDate = df.format(new Date());// new Date()为获取当前系统时间
			basebean.writeLog("==========当前日期: "+sysDate);
			//循环每一天
			for(String date = startDate;dayDiff(date,endDate)>=0;date=addDate(date,1)){//根据日期循环				
				basebean.writeLog("================日期： "+date);
				
				String sql4 = "select (select (case when to_char(sysdate,'MM-dd')>='04-01'" +
						" then 0 " +
						" else (nvl(a.pych,0)-nvl(b.jbhours,0))" +
						" end) as hours" +
						" from (select pemn,pych from hrqw_empinfo where pemn="+pemn+") a " +
						" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours " +
						" from hrqw_dutydata" +
						" where pemn="+pemn+" and defitem2 = 'N' and JBID='S' and substr(dutydate,0,4)=to_char(sysdate,'yyyy')  " +
						" and substr(dutydate,6,5) between '01-01' and '03-31'" +
						" group by pemn) b " +
						" on a.pemn=b.pemn ) as pych, " +
						" (select (case when to_char(sysdate,'MM-dd')>='04-01'  or  (a.pysh-b.jbhours+c.overtimehours)<0" +
						" then 0 " +
						" else (nvl(a.pysh,0)-nvl(b.jbhours,0)+nvl(c.overtimehours,0))" +
						" end) as hours" +
						" from (select pemn,pysh from hrqw_empinfo where pemn="+pemn+") a " +
						" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours     " +
						" from hrqw_dutydata " +
						" where pemn="+pemn+" and defitem2 = 'N' and JBID='C' and substr(dutydate,0,4)=to_char(sysdate,'yyyy') " +
						" and substr(dutydate,6,5) between '01-01' and '03-31'" +
						" group by pemn) b " +
						" on a.pemn=b.pemn " +
						" left join (select pemn,sum(overtimehours) as overtimehours    " +
						" from hrqw_dutydata " +
						" where pemn="+pemn+" and overtimeflag is null and substr(dutydate,0,4)=to_char(sysdate,'yyyy')" +
						" group by pemn) c" +
						" on a.pemn=c.pemn) as pysh," +
						" (select nvl(a.pmch,0)-nvl(b.jbhours,0) as hours" +
						" from (select pemn,pmch from hrqw_empinfo where pemn="+pemn+") a " +
						" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours " +
						" from hrqw_dutydata" +
						" where pemn="+pemn+" and defitem2 = 'N' and JBID='S' and substr(dutydate,0,4)=to_char(sysdate,'yyyy')" +
						" group by pemn) b " +
						" on a.pemn=b.pemn) as pmch," +
						" (select nvl(a.pmsh,0)-nvl(b.jbhours,0)+nvl(c.overtimehours,0) as hours" +
						" from (select pemn,pmsh from hrqw_empinfo where pemn="+pemn+") a " +
						" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours      " +
						" from hrqw_dutydata " +
						" where pemn="+pemn+" and defitem2 = 'N' and JBID='C' and substr(dutydate,0,4)=to_char(sysdate,'yyyy')" +
						" group by pemn) b " +
						" on a.pemn=b.pemn " +
						" left join (select pemn,sum(overtimehours) as overtimehours    " +
						" from hrqw_dutydata " +
						" where pemn="+pemn+" and overtimeflag is null and substr(dutydate,0,4)=to_char(sysdate,'yyyy')" +
						" group by pemn) c" +
						" on a.pemn=c.pemn) as pmsh," +
						" (select a.sjlasthours-nvl(b.jbhours,0) as hours" +
						" from (select pemn,sjlasthours from hrqw_empinfo where pemn="+pemn+") a " +
						" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours " +
						" from hrqw_dutydata" +
						" where pemn="+pemn+" and defitem2 = 'N' and JBID='V' " +
						" group by pemn) b " +
						" on a.pemn=b.pemn) as sjlasthours" +
						" from dual";
				rs4.executeSql(sql4);
				basebean.writeLog("============sql4"+sql4);				
				
				pych = 0;	//去年剩余年休
				pysh = 0;	//去年剩余调休
				pmch = 0;	//剩余年休
				pmsh = 0;	//剩余调休
				sjlasthours = 0;	//剩余事假
				while(rs4.next()){
					pych = rs4.getDouble(1);
					pysh = rs4.getDouble(2);
					pmch = rs4.getDouble(3);
					pmsh = rs4.getDouble(4);
					sjlasthours = rs4.getDouble(5);
				}		
								
				String sql2 = "select ondutydate,offdutydate,shiftid,JBID,lateflag,leaveflag " +
						" from hrqw_dutydata where pemn="+pemn+" and dutydate = '"+date+"'";
				rs2.executeSql(sql2);
				basebean.writeLog("===================sql2："+ sql2);
				String ondutydate = null;
				String offdutydate = null;
				String shiftid = null;
				String JBID = null;
				String lateflag = null;
				String leaveflag = null;
				while(rs2.next()){
					ondutydate = rs2.getString(1);
					if(!"".equals(ondutydate)){
						ondutydate = ondutydate.substring(0, 2)+":"+ondutydate.substring(2, 4);
						}
					offdutydate = rs2.getString(2);	
					if(!"".equals(offdutydate)){
						offdutydate = offdutydate.substring(0, 2)+":"+offdutydate.substring(2, 4);
					}
					shiftid = rs2.getString(3);
					JBID =rs2.getString(4);
					lateflag = rs2.getString(5);
					leaveflag = rs2.getString(6);					
				}		
				
				String sql3 = "select starttime,endtime from HRQW_SHIFT where shiftid='"+shiftid+"'";
				rs3.executeSql(sql3);
				basebean.writeLog("===================sql3："+ sql3);
				String starttime = null;	//规则上班开始时间
				String endtime = null;		//规则下班时间
				while(rs3.next()){
					starttime = rs3.getString(1);
					endtime = rs3.getString(2);
					basebean.writeLog("===================starttime : endtime"+ starttime+" : "+endtime);
				}
				
				if(shiftid.equals("1")){
					double hours;
					try {
						hours = (Double) json.get(date);	//请假时长
					} catch (NullPointerException npe) {
						basebean.writeLog("=========请假时长为0:"+npe);
						hours=0;
					}			 
				//回写假别和请假小时数
				sql = "update hrqw_dutydata set JBID='"+JBIDs+"', jbhours="+hours+" where pemn="+pemn+" and dutydate='"+date+"' ";
				rs.executeSql(sql);
				basebean.writeLog("===================回写假别和请假小时数sql: "+ sql);
				
				if(dayDiff(date, sysDate)>=0){
				//是否去标识和加标识
				if(timeCompare(starttime,ondutydate)>0 && timeCompare(starttime,startTime)>0){//加迟到
					sql = "update hrqw_dutydata set lateflag='#' where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================加迟到sql: "+ sql);
				}else if(timeCompare(starttime,ondutydate)<=0 || (timeCompare(starttime,startTime)<=0 && timeCompare(ondutydate,endTime)>=0)){//去迟到
					sql = "update hrqw_dutydata set lateflag=null where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================去迟到sql: "+ sql);
				}
				if(timeCompare(endtime,offdutydate)<0 && timeCompare(endtime,endTime)<0){//加早退
					sql = "update hrqw_dutydata set leaveflag='#' where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================加早退sql: "+ sql);
				}else if(timeCompare(endtime,offdutydate)>=0 || (timeCompare(endtime,endTime)>=0 && timeCompare(offdutydate,startTime)<=0)){//去早退
					sql = "update hrqw_dutydata set leaveflag=null where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================去早退sql: "+ sql);
				}
				}
			}else if(shiftid.equals("7")){
				double hours;
				try {
					hours = (Double) json.get(date);	//请假时长
				} catch (NullPointerException npe) {
					basebean.writeLog("=========请假时长为0:"+npe);
					hours=0;
				}
				if(JBIDs.equals("change")){//免加班
				if(hours<=4){	
					sql = "update hrqw_dutydata set shiftid='2',overtimehours=overtimehours+"+(4-hours)+",overtimeflag='X' where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================7班别免加班sql: "+ sql);
				}else{
					if(pych+4-hours>=0){	//去年年假充足
						sql = "update hrqw_dutydata set shiftid='2',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================7班别免加班去年年休sql: "+ sql);
					}else if(pysh+4-hours>=0){	//去年调休充足
						sql = "update hrqw_dutydata set shiftid='2',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================7班别免加班去年调休sql: "+ sql);
					}else if(pmch+4-hours>=0){	//年休充足
						sql = "update hrqw_dutydata set shiftid='2',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================7班别免加班年休sql: "+ sql);
					}else if(pmsh+4-hours>=0){	//调休充足
						sql = "update hrqw_dutydata set shiftid='2',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================7班别免加班调休sql: "+ sql);
					}else{	//去D处理
						RecordSetDataSource rsD = new RecordSetDataSource("HR");
						String sqlD = "select dutydate from hrqw_dutydata " +
										" where pemn="+pemn+" and dutydate>'"+date+"' and JBID='D' and defitem2='N'" +
										" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
										" order by dutydate";
						rsD.executeSql(sqlD);
						basebean.writeLog("===================向后找D的sqlD: "+ sqlD);
						String dutydateD = null;
						if(rsD.next()){
							dutydateD = rsD.getString(1);
						}else{
							sqlD = "select dutydate from hrqw_dutydata " +
									" where pemn="+pemn+" and dutydate<'"+date+"' and JBID='D' and defitem2='N'" +
									" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
									" order by dutydate desc";
							rsD.executeSql(sqlD);
							basebean.writeLog("===================向前找D的sqlD: "+ sqlD);
							if(rsD.next()){
								dutydateD = rsD.getString(1);
							}else{
								dutydateD="error";
							}
						}
						
						if(hours==12){//请假一整天
							if(!dutydateD.equals("error")){
								sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================7班别去D的sql: "+ sql);
								sql = "update hrqw_dutydata set JBID='R' where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================7班把请假当天改R的sql: "+ sql);
							}else{
								if(sjlasthours+4-hours>=0){//事假充足
									sql = "update hrqw_dutydata set shiftid='2',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================7班别请事假的sql: "+ sql);
								}else{
									sql = "update hrqw_dutydata set shiftid='2',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================7班别请病假的sql: "+ sql);
								}
							}
						}else{	//请假不是一整天的
							if(!dutydateD.equals("error")){
								sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================7班别去D的sql: "+ sql);
								sql = "update hrqw_dutydata set shiftid='6',JBID='D',overtimehours="+(12-hours)+",overtimeflag='X'" +
										" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================7班改成D的加班处理sql: "+ sql);
							}else{
								if(sjlasthours+4-hours>=0){//事假充足
									sql = "update hrqw_dutydata set shiftid='2',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================7班别请事假的sql: "+ sql);
								}else{
									sql = "update hrqw_dutydata set shiftid='2',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================7班别请病假的sql: "+ sql);
								}
							}
						}
					}
				}					
			}
		}else if (shiftid.equals("8")){	//8班别
			
			if(date.equals(startDate)){	//是开始日期
				double hourl;
				double hours;
				try {
					hourl = (Double) json.get(addDate(date, -1));	//昨天的请假时长
				} catch (NullPointerException npe) {
					basebean.writeLog("=========昨天的请假时长为0:"+npe);
					hourl=0;
				}
				try {
					hours = (Double) json.get(date);	//今天的请假时长
				} catch (NullPointerException npe) {
					basebean.writeLog("=========今天的请假时长为0:"+npe);
					hours=0;
				}
			
			if(JBIDs.equals("change")){	//免加班
			if(hourl>0){	//昨天的时长大于0
				if(hourl<=4){	
					sql = "update hrqw_dutydata set shiftid='5',overtimehours=overtimehours+"+(4-hourl)+",overtimeflag='X' where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================8班别免加班sql: "+ sql);
				}else{
					if(pych+4-hourl>=0){	//去年年假充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班去年年休sql: "+ sql);
					}else if(pysh+4-hourl>=0){	//去年调休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班去年调休sql: "+ sql);
					}else if(pmch+4-hourl>=0){	//年休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班年休sql: "+ sql);
					}else if(pmsh+4-hourl>=0){	//调休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班调休sql: "+ sql);
					}else{	//去D处理
						RecordSetDataSource rsD = new RecordSetDataSource("HR");
						String sqlD = "select dutydate from hrqw_dutydata " +
										" where pemn="+pemn+" and dutydate>'"+addDate(date, -1)+"' and JBID='D' and defitem2='N'" +
										" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
										" order by dutydate";
						rsD.executeSql(sqlD);
						basebean.writeLog("===================向后找D的sqlD: "+ sqlD);
						String dutydateD = null;
						if(rsD.next()){
							dutydateD = rsD.getString(1);
						}else{
							sqlD = "select dutydate from hrqw_dutydata " +
									" where pemn="+pemn+" and dutydate<'"+addDate(date, -1)+"' and JBID='D' and defitem2='N'" +
									" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
									" order by dutydate desc";
							rsD.executeSql(sqlD);
							basebean.writeLog("===================向前找D的sqlD: "+ sqlD);
							if(rsD.next()){
								dutydateD = rsD.getString(1);
							}else{
								dutydateD="error";
							}
						}
						
							//昨天的请假肯定不是一整天的
							if(!dutydateD.equals("error")){
								sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别去D的sql: "+ sql);
								sql = "update hrqw_dutydata set shiftid='6',JBID='D',overtimehours="+(12-hourl)+",overtimeflag='X'" +
										" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班改成D的加班处理sql: "+ sql);
							}else{
								if(sjlasthours+4-hourl>=0){//事假充足
									sql = "update hrqw_dutydata set shiftid='5',JBID='V',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别前一天请事假的sql: "+ sql);
								}else{
									sql = "update hrqw_dutydata set shiftid='5',JBID='W',jbhours="+(hourl-4)+" where pemn="+pemn+" and dutydate='"+addDate(date,-1)+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别前一天请病假的sql: "+ sql);
								}
							}				
					}
				}
			}
			if(hours>0){	//今天的时长大于0
				if(hours<=4){	
					sql = "update hrqw_dutydata set shiftid='5',overtimehours=overtimehours+"+(4-hours)+",overtimeflag='X' where pemn="+pemn+" and dutydate='"+date+"' ";
					rs.executeSql(sql);
					basebean.writeLog("===================8班别免加班sql: "+ sql);
				}else{
					if(pych+4-hours>=0){	//去年年假充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班去年年休sql: "+ sql);
					}else if(pysh+4-hours>=0){	//去年调休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班去年调休sql: "+ sql);
					}else if(pmch+4-hours>=0){	//年休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班年休sql: "+ sql);
					}else if(pmsh+4-hours>=0){	//调休充足
						sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
						rs.executeSql(sql);
						basebean.writeLog("===================8班别免加班调休sql: "+ sql);
					}else{	//去D处理
						RecordSetDataSource rsD = new RecordSetDataSource("HR");
						String sqlD = "select dutydate from hrqw_dutydata " +
										" where pemn="+pemn+" and dutydate>'"+date+"' and JBID='D' and defitem2='N'" +
										" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
										" order by dutydate";
						rsD.executeSql(sqlD);
						basebean.writeLog("===================向后找D的sqlD: "+ sqlD);
						String dutydateD = null;
						if(rsD.next()){
							dutydateD = rsD.getString(1);
						}else{
							sqlD = "select dutydate from hrqw_dutydata " +
									" where pemn="+pemn+" and dutydate<'"+date+"' and JBID='D' and defitem2='N'" +
									" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
									" order by dutydate desc";
							rsD.executeSql(sqlD);
							basebean.writeLog("===================向前找D的sqlD: "+ sqlD);
							if(rsD.next()){
								dutydateD = rsD.getString(1);
							}else{
								dutydateD="error";
							}
						}
						
							//今天的可能是一整天
						if(hours==12){//请假一整天
							if(!dutydateD.equals("error")){
								sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别去D的sql: "+ sql);
								sql = "update hrqw_dutydata set JBID='R' where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班把请假当天改R的sql: "+ sql);
							}else{
								if(sjlasthours+4-hours>=0){//事假充足
									sql = "update hrqw_dutydata set shiftid='5',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别请事假的sql: "+ sql);
								}else{
									sql = "update hrqw_dutydata set shiftid='5',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别请病假的sql: "+ sql);
								}
							}
						}else{//时长不足一天
							if(!dutydateD.equals("error")){
								sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别去D的sql: "+ sql);
								sql = "update hrqw_dutydata set shiftid='6',JBID='D',overtimehours="+(12-hours)+",overtimeflag='X'" +
										" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班改成D的加班处理sql: "+ sql);
							}else{
								if(sjlasthours+4-hours>=0){//事假充足
									sql = "update hrqw_dutydata set shiftid='5',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别今天请事假的sql: "+ sql);
								}else{
									sql = "update hrqw_dutydata set shiftid='5',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
									rs.executeSql(sql);
									basebean.writeLog("===================8班别今天请病假的sql: "+ sql);
								}
							}	
							}
						}
					}
				}
			}
			}else{//不是开始日的，只要判断请假时长是否是0，不是的就执行操作
				double hours;
				try {
					hours = (Double) json.get(date);	//请假时长
				} catch (NullPointerException npe) {
					basebean.writeLog("=========请假时长为0:"+npe);
					hours=0;
				}
				
				if(JBIDs.equals("change")){	//免加班
					
					if(hours>0){	//今天的时长大于0
						if(hours<=4){	
							sql = "update hrqw_dutydata set shiftid='5',overtimehours=overtimehours+"+(4-hours)+",overtimeflag='X' where pemn="+pemn+" and dutydate='"+date+"' ";
							rs.executeSql(sql);
							basebean.writeLog("===================8班别免加班sql: "+ sql);
						}else{
							if(pych+4-hours>=0){	//去年年假充足
								sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别免加班去年年休sql: "+ sql);
							}else if(pysh+4-hours>=0){	//去年调休充足
								sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别免加班去年调休sql: "+ sql);
							}else if(pmch+4-hours>=0){	//年休充足
								sql = "update hrqw_dutydata set shiftid='5',JBID='S',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别免加班年休sql: "+ sql);
							}else if(pmsh+4-hours>=0){	//调休充足
								sql = "update hrqw_dutydata set shiftid='5',JBID='C',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
								rs.executeSql(sql);
								basebean.writeLog("===================8班别免加班调休sql: "+ sql);
							}else{	//去D处理
								RecordSetDataSource rsD = new RecordSetDataSource("HR");
								String sqlD = "select dutydate from hrqw_dutydata " +
												" where pemn="+pemn+" and dutydate>'"+date+"' and JBID='D' and defitem2='N'" +
												" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
												" order by dutydate";
								rsD.executeSql(sqlD);
								basebean.writeLog("===================向后找D的sqlD: "+ sqlD);
								String dutydateD = null;
								if(rsD.next()){
									dutydateD = rsD.getString(1);
								}else{
									sqlD = "select dutydate from hrqw_dutydata " +
											" where pemn="+pemn+" and dutydate<'"+date+"' and JBID='D' and defitem2='N'" +
											" and (shiftid='7' or shiftid='8') and overtimehours=0 and jbhours=0"+
											" order by dutydate desc";
									rsD.executeSql(sqlD);
									basebean.writeLog("===================向前找D的sqlD: "+ sqlD);
									if(rsD.next()){
										dutydateD = rsD.getString(1);
									}else{
										dutydateD="error";
									}
								}
								
									
								if(hours==12){//请假一整天
									if(!dutydateD.equals("error")){
										sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
										rs.executeSql(sql);
										basebean.writeLog("===================8班别去D的sql: "+ sql);
										sql = "update hrqw_dutydata set JBID='R' where pemn="+pemn+" and dutydate='"+date+"' ";
										rs.executeSql(sql);
										basebean.writeLog("===================8班把请假当天改R的sql: "+ sql);
									}else{
										if(sjlasthours+4-hours>=0){//事假充足
											sql = "update hrqw_dutydata set shiftid='5',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
											rs.executeSql(sql);
											basebean.writeLog("===================8班别请事假的sql: "+ sql);
										}else{
											sql = "update hrqw_dutydata set shiftid='5',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
											rs.executeSql(sql);
											basebean.writeLog("===================8班别请病假的sql: "+ sql);
										}
									}
								}else{//时长不足一天
									if(!dutydateD.equals("error")){
										sql = "update hrqw_dutydata set JBID=null where pemn="+pemn+" and dutydate='"+dutydateD+"' ";
										rs.executeSql(sql);
										basebean.writeLog("===================8班别去D的sql: "+ sql);
										sql = "update hrqw_dutydata set shiftid='6',JBID='D',overtimehours="+(12-hours)+",overtimeflag='X'" +
												" where pemn="+pemn+" and dutydate='"+date+"' ";
										rs.executeSql(sql);
										basebean.writeLog("===================8班改成D的加班处理sql: "+ sql);
									}else{
										if(sjlasthours+4-hours>=0){//事假充足
											sql = "update hrqw_dutydata set shiftid='5',JBID='V',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
											rs.executeSql(sql);
											basebean.writeLog("===================8班别今天请事假的sql: "+ sql);
										}else{
											sql = "update hrqw_dutydata set shiftid='5',JBID='W',jbhours="+(hours-4)+" where pemn="+pemn+" and dutydate='"+date+"' ";
											rs.executeSql(sql);
											basebean.writeLog("===================8班别今天请病假的sql: "+ sql);
										}
													}	
												}
											}
										}
									}
								}
							}		
						}			
					}
									
			} catch (Exception e) {
			basebean.writeLog("==============错误信息： "+e);
		}		
		return Action.SUCCESS;
	}
	
public JSONObject diffDay(String pemn ,String startDate, String startTime, String endDate, String endTime){				
		
		basebean.writeLog("===================数据："+ pemn+" , "+startDate+" , "+startTime+" ,"+ endDate+" , "+endTime);
		//JSONArray jsonArr = new JSONArray();
		double z = 0;//做一个double类型的0，解决类型转换异常
		Map<Integer, String> shiftids = new HashMap<Integer, String>();
		Map<Integer, String> JBIDs = new HashMap<Integer, String>();
		Map<Integer, String> dutydates = new HashMap<Integer, String>();
		JSONObject json3 = new JSONObject();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");
		RecordSetDataSource ds2 = new RecordSetDataSource("HR");
		RecordSetDataSource ds3 = new RecordSetDataSource("HR");
		//班别
		String sql1 = "select (case when shiftid is null then '-1' else shiftid end ),dutydate " +
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
			dutydates.put(i, ds1.getString(2));
			basebean.writeLog("===================shiftids["+i+"] : dutudates["+i+"] = "+ shiftids.get(i)+" : "+dutydates.get(i));
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
		double day = 0;	//请假天数
		double hour1 = 0;//第一天请假的小时数
		double hour2 = 0;//中间天请假的小时数
		double hour3 = 0;//最后一天请假的小时数
		basebean.writeLog("===================请假跨越的天数："+ (days+1));
		
		try {
			String starttime1 = null;	//请假开始时间8班别规则上班开始时间
			String endtime1 = null;		//请假开始时间8班别规则下班时间
			String starttime2 = null;	//请假结束时间8班别规则上班开始时间
			String endtime2 = null;		//请假结束时间8班别规则下班时间
			String starttime = null;	//规则上班开始时间
			String endtime = null;		//规则下班时间
			String afterTime = null;		//休息结束时间
			double dutyhours = 0;		//规则对应的上班小时数
			
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
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					dutyhours = ds3.getDouble(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				double dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				double resthour = dutyhours-dutyhour;	//休息时长
				String midtime = addTime(afterTime, resthour);	//休息结束时间
				basebean.writeLog("===================打卡规则计算的上班总时长:"+ dutyhour+"===================休息时长:"+ resthour+"===================休息开始时间:"+ midtime);				
				
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
							json3.put(addDate(startDate, -1), hour1);
						}else if(timeCompare("08:00", startTime)<0 && timeCompare("08:00", endTime)>=0){
							if(shiftidl.equals("8")){
								if(JBIDl.equals("R") || JBIDl.equals("T")){
									hour1 = 0;
									hour3 = 0;
								}else{
									hour1 = timeDiff(startTime, endtime);
									hour3 = 0;
									json3.put(addDate(startDate, -1), hour1);
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
									json3.put(addDate(startDate, -1), timeDiff(startTime, endtime));
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
					
					try {
						if((Double) json3.get(addDate(startDate, -1))>0){	//昨天的时间不为0
							json3.put(startDate, hour1-(Double) json3.get(addDate(startDate, -1)));
						}
					} catch (NullPointerException npe) {
						basebean.writeLog("==========昨天的时间为0"+npe);
						json3.put(startDate, hour1);
					}
					
						
				}else if(JBID.equals("R") || JBID.equals("T")){	//假日就等于O
					basebean.writeLog("=============假日： "+JBID);
					hour1 = 0;
					hour3 = 0;
					json3.put(startDate, hour1);
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
					json3.put(startDate, hour1);
				}
				
				double hour = hour1+hour2+hour3;
				day = hour/dutyhours;		//请假天数
				//同一天的时间插入
				
					
				
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
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(afterTime.equals("00:00")) afterTime = "23:59";
					dutyhours = ds3.getDouble(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				double dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				double resthour = dutyhours-dutyhour;	//休息时长
				String midtime = addTime(afterTime, resthour);	//休息结束时间
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
							json3.put(addDate(startDate, -1), hour1);
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
									json3.put(addDate(startDate, -1), timeDiff(startTime, endtime));
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
						hour1 = timeDiff(startTime,endtime)+resthour;			
					}else if(timeCompare(midtime, startTime)>0 && timeCompare(afterTime, startTime)<=0){	//开始时间在休息时间
						hour1 = timeDiff(afterTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0 && timeCompare(endtime, startTime)<=0){	//开始时间休息结束时间和下班时间之间
						hour1 = timeDiff(startTime,endtime);	
					}else if(timeCompare(afterTime, startTime)>0){	//开始时间在下班之后
						hour1 = 0;	
					}
				}
				double day1 = 0;
				day1 = hour1/dutyhours;
				//第一天时间插入
				try {
					if((Double) json3.get(addDate(startDate, -1))>0){	//昨天的时间不为0
						json3.put(startDate, hour1-(Double) json3.get(addDate(startDate, -1)));
					}
				} catch (NullPointerException npe) {
					basebean.writeLog("==========昨天的时间为0"+npe);
					json3.put(startDate, hour1);
				}
				
				 
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
					afterTime = ds3.getString(3);	//对应的是数据库表中的midtime
					if(afterTime.equals("00:00")) afterTime = "23:59";
					dutyhours = ds3.getDouble(4);
					basebean.writeLog("===================starttime:"+ starttime+"===================endtime:"+ endtime+"===================afterTime:"+ afterTime+"===================dutyhours:"+ dutyhours);

				}				
				dutyhour = timeDiff(starttime,endtime);	//打卡规则计算的上班总时长
				resthour = dutyhours-dutyhour;	//休息时长
				midtime = addTime(afterTime, resthour);	//休息结束时间
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
									hour3 = datetimeDiff(starttime2, endTime1);
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
					hour3 = hour3-dutyhours;
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
						hour3 = timeDiff(starttime,endTime)+resthour;	
					}else if(timeCompare(afterTime, endTime)>0){		//结束时间在下班之后
						hour3 = dutyhours;	
					}			
				}
				
				double day3 = 0;
				day3 = hour3/dutyhours;
								
				double day2 = 0;
				//中间时间
				if(days>=2){
					String date = addDate(startDate,1);
				for(int j = 1; j<days; j++,  addDate(date, 1)){
					
					double hour = 0;
					String shiftid = shiftids.get(j);
					String JBID = JBIDs.get(j);
					basebean.writeLog("===================shiftids["+j+"]"+ shiftid+"=====JBIDs["+j+"]"+ JBID);
					sql3 = "select starttime,endtime,midtime,defitem2 from HRQW_SHIFT where shiftid='"+shiftid+"'";
					ds3.executeSql(sql3);			
					basebean.writeLog("===================sql3："+ sql3);				
					while(ds3.next()){					
						dutyhours = ds3.getDouble(4);
					}
					
					if(JBID.equals("R") || JBID.equals("T")){
						basebean.writeLog("=============假日： "+JBID);
						hour = 0;
					}else{										
						
						hour = dutyhours;
					}
					//中间时间循环插入
					json3.put(date, hour);
					day2 = day2+hour/dutyhours;
					hour2 = hour2+hour;		
					
					
				}
			}
				basebean.writeLog("===================hour1: hour2:　hour3:"+ hour1+":"+hour2+":"+hour3);
				day = day1+day2+day3;
				//最后一天时间插入
				
				if(hour3<=0){
					
					json3.put(endDate, z);
					json3.put(addDate(endDate, -1), dutyhours+hour3);
				}else{
					json3.put(endDate, hour3);
				}
			}
			
			double hourTotal = hour1+hour2+hour3;	//请假时长
			double dayTotal = day;	//请假时长
			BigDecimal l=new BigDecimal(Double.toString(dayTotal));
			dayTotal=(double) l.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
			json3.put("hourTotal", hourTotal);
			json3.put("dayTotal", dayTotal);
			json3.put("error", 1);
		} catch (NullPointerException npe){
			basebean.writeLog("===================异常： "+npe);
			json3.put("error", 0);
			//basebean.writeLog("===================日期对应的班别或者假别没数据");			
		}catch (Exception e){
			basebean.writeLog("=======异常： "+e);
			//e.printStackTrace();			
		}	
			
		return json3;
	
	}
	
	//计算日期时间差值
	public double datetimeDiff(String startTime, String endTime){
		basebean.writeLog("===================startTime , endTime  "+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("yyyy-MM-dd HH:mm"); 
		double hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  System.out.println("===================begin:  end"+ begin+" : "+end);
		  System.out.println("===================begin:  end.gettime"+ begin.getTime()+" : "+end.getTime());
		  double   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
		  System.out.println("===================between："+ between);
		  hour=between/3600;   
		}catch (Exception e)   
		{   
		}
		basebean.writeLog("===================hour："+ hour);
		
		return hour;
	}
	
	//计算时间差值
	public double timeDiff(String startTime, String endTime){
		basebean.writeLog("===================startTime , endTime  "+ startTime+" , "+endTime);
		SimpleDateFormat   df   =   new   SimpleDateFormat("HH:mm"); 
		double hour = 0;
		try{
		  Date   begin=df.parse(startTime);   
		  Date   end   =   df.parse(endTime);
		  double   between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
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
		  double  between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
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
			
			  double  between=(end.getTime()-begin.getTime())/1000;//除以1000是为了转换成秒   
			  days=(int) (between/(24*3600));   	  
			}catch (Exception e)   
			{   
			}
			basebean.writeLog("===================days："+ days);
			return days;	  
		} 
	
	//时间加上diff小时数
	public String addTime(String time, double diff ){
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
