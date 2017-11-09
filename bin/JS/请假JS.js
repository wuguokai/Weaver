<!-- script代码，如果需要引用js文件，请使用与HTML中相同的方式。 -->
<style>
   input{ border: 0px !important; }
   select{ width:90%;border: 0px !important; }
   textarea{width: 98% !important;border:1px solid #4a86e8 !important; }
   .textarea{min-height:72px !important;height:auto !important; width: 98% !important;border:1px solid #4a86e8 !important; }
   .e8_innerShow{ border: 0px !important; }
   .e8_outScroll{ border: 0px !important; }
   .cusbrowwidth1 .e8_os{min-width:70px !important;}
   .cusbrowwidth .e8_os{min-width:53px !important;}
</style>
<script type="text/javascript">
    /*
    *  TODO
     *  请在此处编写javascript代码
     */

jQuery(document).ready(function (){
	
	checkCustomize = function () {
		var leave_hours = jQuery("#field6817").val();	//请假时长
		var shiftid = jQuery("#field6737").val();		//班别
		var pmch = jQuery("#field6925").val();			//剩余年休
		var pmsh = jQuery("#field6892").val();			//剩余调休
		var sjlasthours = jQuery("#field6893").val();	//剩余事假
		var bjlasthours = jQuery("#field12742").val();	//剩余病假
		var pych = jQuery("#field6913").val();			//去年剩余年休
		var pysh = jQuery("#field6891").val();			//去年剩余调休
		var startDate = jQuery("#field6693").val();		//请假开始日期
		var startTime = jQuery("#field6694").val();		//请假开始时间
		var endDate = jQuery("#field6695").val();		//请假结束日期
		var endTime = jQuery("#field6647").val();		//请假结束时间
		var classify = jQuery("#field6644").val();		//请假类别
		var pemn = jQuery("#field6789").val();			//到职号
		var flag = true;
		//alert(sjlasthours);
		//alert(bjlasthours);
		//alert(leave_hours);

//增加提交时判断结束必须大于开始，以及判断公假时可不做后续防呆（开始）		
			 var state= cus_CompareTime("field6693,field6694", "field6695,field6647");

    if(!state){

        window.top.Dialog.alert("结束时间必须大于开始时间");

        return false;

    }
	
	if(shiftid != '1' && shiftid != '7' && shiftid != '8' ){
		alert("请假日期不是1，7，8班别，请联系HR！")
		return false;
	}

			if(classify=='1'){
			return true;
			}
//增加提交时判断结束必须大于开始，以及判断公假时可不做后续防呆（结束）

		//alert(pemn+" : "+startDate+" : "+startTime+" : "+endDate+" : "+endTime);
		//if(pemn!='' && startDate!='' && startTime=''&& endDate!=''&& endTime!=''){
			jQuery.ajax({
			url:"/wugk/CheckSubmit.jsp?t="+new Date(),
			type:"POST",
			dataType:"json",
			async:false,
			data:{
				pemn:pemn,
				startDate:startDate,
				startTime:startTime,
				endDate:endDate,
				endTime:endTime
			},
			success:function(datasTemp){
				//alert(1111);
				//alert("提交验证check： "+datasTemp[0].check);
			   if(!datasTemp[0].check){
					alert("您请假的时间段有误，不可跨班别请假或打卡数据不全，请核对或联系HR！")
					flag = false;  
				}
			}
				});
				
		if(!flag){
			return flag;
		}



		if(leave_hours=='' || leave_hours==0 ){
			alert("您的请假时长未计算，请重新选择日期或联系HR");
			return false;
		}else if(shiftid=='1'){
			if(pych-leave_hours>=0){	//去年年假充足
				if(classify!=3){
					alert("您去年的年休还未使用完，不能选择其他假别！");
					return false;
				}
			}else if(pysh-leave_hours>=0){		//去年调休充足
				if(classify!=4){
					alert("您去年的调休还未使用完，不能选择其他假别！");
					return false;
				}			
			}else if(pmch-leave_hours>=0){		//今年年假充足
				if(classify!=3){
					alert("您今年的年休还未使用完，不能选择其他假别！");
					return false;
				}
			}else if(pmsh-leave_hours>=0){		//今年调休充足
				if(classify!=4){
					alert("您今年的调休还未使用完，不能选择其他假别！");
					return false;
				}
			}else if(((sjlasthours-leave_hours>=0 && bjlasthours-leave_hours>=0) && (classify!=0 && classify!=2)) || ((sjlasthours-leave_hours>=0 && bjlasthours-leave_hours<0) && classify!=0) || ((sjlasthours-leave_hours<0 && bjlasthours-leave_hours>=0) && classify!=2)){	//剩余事假或病假充足
			
		//这边主要要判断的是事假或者病假足够的时候没有请病假或事假	
		//((sjlasthours-leave_hours>=0 && bjlasthours-leave_hours>=0) && (classify!=0 && classify!=2)) || ((sjlasthours-leave_hours>=0 && bjlasthours-leave_hours<0) && classify!=0) || ((sjlasthours-leave_hours<0 && bjlasthours-leave_hours>=0) && classify!=2)
//			三种情况：
//			(事假充足且病假充足)且(没选事假或病假)
//		或	(事假充足且病假不足)且(没选事假)
//		或	(事假不足且事假充足)且(没选病假)
					alert("您剩余的事假/病假还未使用完，不能选择其他假别！");
					return false;
			}else if(pych-leave_hours<0 && pysh-leave_hours<0 && pmch-leave_hours<0 && pmsh-leave_hours<0 && sjlasthours-leave_hours<0 && bjlasthours-leave_hours<0 && classify!=6  ){//所有时长都不足，只能请无薪假
				alert("您剩余的年休/调休/事假/病假时长不足，请选择无薪假！");	//、事假
				return false;
			}	
		}else if(shiftid=='7' || shiftid=='8'){
			if(classify != '5'  && classify != '1'){
//alert(classify);
				alert("您对应的班别只能选择免加班或者公假！");
				return false;
			}
		}
		return true;
	}
});


jQuery(document).ready(function () {

	var sc = jQuery("#field6817");
	var ts = jQuery("#field6646");
	sc.attr("readOnly","readOnly");
	ts.attr("readOnly","readOnly");

	
  jQuery("#field6789,#field6693,#field6694,#field6695,#field6647").bindPropertyChange(function(){
	
	var pemn1 = jQuery("#field6789").val();
	var startDate1 = jQuery("#field6693").val();
	var startTime1 = jQuery("#field6694").val();
	var endDate1 = jQuery("#field6695").val();
	var endTime1 = jQuery("#field6647").val();
	
	if(pemn1!='' && startDate1!='' && startTime1!=''&& endDate1!=''&& endTime1!=''){
		//alert(pemn1+" : "+startDate1+" : "+startTime1+" : "+endDate1+" : "+endTime1);
		
		var begin = startDate1;
		var arr = begin.split("-");
		var bs    = arr[0] + "/" + arr[1] + "/" + arr[2]; //开始日期字符串

		var begint = startTime1;
		var arrt = begint.split(":");
		var bst    = arrt[0] + "/" + arrt[1] ; //开始时间字符串

		var end  =  endDate1;
		var arrs = end.split("-");
		var es    = arrs[0] + "/" + arrs[1] + "/" + arrs[2]; //结束日期字符串

		var endt =  endTime1;
		var arrst = endt.split(":");
		var est    = arrst[0] + "/" + arrst[1] ; //结束时间字符串
		
		//alert("bs: "+bs+"bst: "+bst+"es: "+es+"est: "+est);
		
		if(es<bs) 
		{
			alert("请假结束日期不能小于开始日期！");
		}
		else if(es==bs && est<=bst)
		{
			alert("请假结束时间不能小于或等于开始时间！");
		}else{
			jQuery.ajax({
			url:"/wugk/LeaveDays.jsp?t="+new Date(),
			type:"POST",
			dataType:"json",
			async:false,
			data:{
				pemn:pemn1,
				startDate:startDate1,
				startTime:startTime1,
				endDate:endDate1,
				endTime:endTime1
			},
			success:function(datasTemp){
				//alert(1111);
			   if(datasTemp[0].error == 0)
				{
					alert("您选择的时间段无卡档数据！");									
				}else{
					//alert("时长： "+datasTemp[0].hourTotal);
					jQuery("#field6817").val(datasTemp[0].hourTotal);
					//jQuery("#field6817span").html(datasTemp[0].hourTotal);
				   
					//alert("天数： "+datasTemp[0].dayTotal);
					jQuery("#field6646").val(datasTemp[0].dayTotal);
					//jQuery("#field6646span").html(datasTemp[0].dayTotal);
				}
					}
				});	
				
				}								
			}				  
  });

});


jQuery(document).ready(function () {

//把选择框改radio单选样式显示
/**
*封装方法：cus_ConvertSelectToRadio(fieldids)
*
* @param {fields}字段id集合，以逗号隔开
*
* 注:转换后可能不支持联动功能，只是转换样式显示/编辑
*/
 cus_ConvertSelectToRadio("field6644,field6692");  


//修改时间控件间隔
jQuery(
	function initTimeChooseDiv(){
	var html=[];
	html.push("<tr>");	
	for(var i=0;i<60;i++){
		var hidden = i%30==0?"":"display:none;"	//显示30的倍数
		html.push('<td width="90" align="center" onmouseover="style.backgroundColor=\'#BEEBEE\'" onmouseout="style.backgroundColor=\'#fff\'" style="font-size: 9pt; font-family: Verdana; cursor: pointer; background-color: rgb(255, 255, 255);'+hidden+'" onclick="getTime(minute'+i+')">'+i+'<input type="hidden" id="minute'+i+'" value="'+i+'"></td>');
	}//循环输出td
	html.push("</tr>");
	jQuery("#meizzDateLayer2").contents().find("#TimeLayer tbody").html(html.join(""));
	jQuery("#meizzDateLayer2").width("200").height("86");
}
	
);

//插入图片，需要在页面中添加对应ID，如icon1
jQuery("#icon1").html('<img id="img1" src="/page/resource/userfile/image/icon/001.png" width="35" height="35"  /> ');
jQuery("#icon2").html('<img id="img2" src="/page/resource/userfile/image/icon/002.png" width="35" height="35" /> ');
jQuery("#icon3").html('<img id="img3" src="/page/resource/userfile/image/icon/003.png" width="35" height="35" /> ');
jQuery("#icon4").html('<img id="img4" src="/page/resource/userfile/image/icon/004.png" width="35" height="35" /> ');
jQuery("#icon5").html('<img id="img5" src="/page/resource/userfile/image/icon/005.png" width="35" height="35" /> ');
jQuery("#icon6").html('<img id="img6" src="/page/resource/userfile/image/icon/006.png" width="35" height="35" /> ');
jQuery("#icon7").html('<img id="img7" src="/page/resource/userfile/image/icon/007.png" width="35" height="35" /> ');
//alert("0");

//添加点击事件，包括图片和标题
	jQuery("#img1,#title1").click(function () {
jQuery(".icon1_h").toggle();
		
	});
jQuery("#img2,#title2").click(function () {
		//alert("1");
jQuery(".icon2_h").toggle();
		
	});

jQuery("#img3,#title3").click(function () {
		//alert("1");
jQuery(".icon3_h").toggle();
		
	});

jQuery("#img4,#title4").click(function () {
		//alert("1");
jQuery(".d1").toggle();
		
	});

jQuery("#img5,#title5").click(function () {
		//alert("1");
jQuery(".e1").toggle();
		
	});

jQuery("#img6,#title6").click(function () {
		//alert("1");
jQuery(".f1").toggle();
		
	});

jQuery("#img7,#title7").click(function () {
		//alert("1");
jQuery(".g1").toggle();
		
	});		
	
});
</script>

