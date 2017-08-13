package com.wugk.kmtc;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weaver.conn.RecordSetDataSource;
import weaver.general.BaseBean;
 
public class LeftTime {
	BaseBean basebean = new BaseBean();
	//计算请假时间
	public String left(String pemn){
						
		basebean.writeLog("===================数据："+ pemn);
		JSONArray jsonArr = new JSONArray();
		JSONObject json1 = new JSONObject();
//		Map<Integer, String> leftTime = new HashMap<Integer, String>();
		RecordSetDataSource ds1 = new RecordSetDataSource("HR");

		String sql1 = "select a.pmch-b.jbhours as hours" +
				" from (select pemn,pmch from hrqw_empinfo where pemn='"+pemn+"') a " +
				" left join (select pemn,sum(case when jbhours=0 then 8 else jbhours end) as jbhours " +
				" from hrqw_dutydata" +
				" where pemn='"+pemn+"' and defitem2 = 'N' and JBID='S' and substr(dutydate,0,4)=to_char(sysdate,'yyyy')" +
				" group by pemn) b " +
				" on a.pemn=b.pemn" ;
		
		ds1.executeSql(sql1);		
		basebean.writeLog("===================sql1："+ sql1);
		
//		String sql2 = "select (case when JBID is null then '-1' else JBID end ) " +
//				" from hrqw_dutydata " +
//				" where pemn="+pemn+" and dutydate between '"+startDate+"' and '"+endDate+"'";
//		
//		ds2.executeSql(sql2);
//		basebean.writeLog("===================sql2："+ sql2);
		
		
		
		while(ds1.next()){	
			json1.put("sjlasthours", ds1.getString(1));
			basebean.writeLog("===================sjlasthours："+ json1.get("sjlasthours")+" : "+ds1.getString(1));
		}
		
//		i=0;
//		while(ds2.next()){
//			JBIDs.put(i, ds2.getString(1));
//			basebean.writeLog("===================JBIDs["+i+"] = "+ JBIDs.get(i));
//			i++;
//		}
//		basebean.writeLog("===================JBIDs的i = "+i);
		jsonArr.add(json1);
		return jsonArr.toString();
	}
	}