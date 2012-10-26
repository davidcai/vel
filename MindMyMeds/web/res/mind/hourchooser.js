$(document).ready(function(){	
	$("DIV.Hour").click(function(ev){
		var $hr = $(ev.currentTarget);
		var m = $hr.attr("m");
		var h = $hr.attr("h");
		if (!m || m=="")
		{
			$hr.attr("m", "0");
			$hr.children("SPAN").text(h+":00");
			$hr.children("INPUT").val("0");
		}
		else if (m<45)
		{
			m = Number(m) + 15;
			$hr.attr("m", m);
			$hr.children("SPAN").text(h+":"+m);
			$hr.children("INPUT").val(m);
		}
		else
		{
			$hr.removeAttr("m");
			$hr.children("SPAN").text(h+":00");
			$hr.children("INPUT").val("");
		}
	}).mousedown(function(ev){
		ev.preventDefault();
	});

});
