$(document).ready(function(){
	// Chat submit
	var $chatCtrl = $("#chatcontrols");
	if ($chatCtrl.length>0)
	{
		$chatCtrl.find("INPUT[type=text]").keypress(function(ev){if (ev.which==13) submitChat(ev);});
		$chatCtrl.find("INPUT[type=submit]").click(function(ev){submitChat(ev);});
	}
});

function submitChat(ev)
{
	var $edit = $("#chatcontrols INPUT[type=text]");
	var q = $edit.val();
	if (jQuery.trim(q)=="") return;
	$edit.val("").focus();
	
	var target = $("#chatcontrols").attr("target");
	
	$.ajax(
		target,
		{
			data: $edit.attr("name") + "=" + encodeURIComponent(q),
			dataType: "html",
			success:
				function(data, textStatus, jqXHR)
				{
					var $box = $("#chatbox");
					$box.append(data);
					//$box.scrollTop($box[0].scrollHeight);
				}
		}
	);
	
	ev.preventDefault();
}
