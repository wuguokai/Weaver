package test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//时间加上diff小时数
		public void addTime(String time, double diff ){
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
//			basebean.writeLog("===================value："+ value);
			System.out.println((5<2 ? 3:4));		}
		
	}

}
