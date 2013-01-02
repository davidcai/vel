UserAgent = UserAgent || {};

$(document).ready(function()
{
	var today = new Date();
	document.cookie = "TZOffset=" + today.getTimezoneOffset() + "; path=/";
	if (window.screen)
	{
		var pixelRatio = (window.devicePixelRatio===undefined) ? 1 : window.devicePixelRatio;
		document.cookie = "Screen=" + window.screen.width + "x" + window.screen.height + "x" + pixelRatio + "; path=/";
	}

	$("INPUT[type=file]").parents("FORM").attr("enctype", "multipart/form-data");
	
//	// Inline form validation
//	$("BODY").on("focus", ".InlineError", function()
//	{
//		$(this).removeClass("InlineError");
//	}).on("blur", "[regexp]", function()
//	{
//		var $input = $(this);
//		if (new RegExp($input.attr("regexp"),"i").test($input.val())==false)
//		{
//			$input.addClass("InlineError");	
//		}
//	}).on("blur", "[mandatory]", function()
//	{
//		var $input = $(this);
//		if ($input.attr("mandatory") && jQuery.trim($input.val())=="")
//		{
//			$input.addClass("InlineError");	
//		}
//	});
	
	// Backwards compatibility with non-HTML5 browsers
	if (!("placeholder" in document.createElement("INPUT")))
	{
		$("BODY").on("focus", "[placeholder]:not(INPUT[type=file])", function()
		{
			var input = $(this);
			if (input.val()==(input.attr("placeholder") + String.fromCharCode(160)))
			{
				input.val("");
				input.removeClass("PlaceHolder");
			}
		}).on("blur", "[placeholder]:not(INPUT[type=file])", function()
		{
			var input = $(this);
			if (input.val()=="")
			{
				input.addClass("PlaceHolder");
				input.val(input.attr("placeholder") + String.fromCharCode(160));
			}
		});
		$("[placeholder]:not(INPUT[type=file])").each(function(index, e)
		{
			var input = $(this);
			if (input.val()=="")
			{
				input.addClass("PlaceHolder");
				input.val(input.attr("placeholder") + String.fromCharCode(160));
			}
		})
		.closest("form").submit(function()
		{
			$(this).find("[placeholder]:not(INPUT[type=file])").each(function()
			{
				var input = $(this);
				if (input.val()==(input.attr("placeholder") + String.fromCharCode(160)))
				{
					input.val("");
				}
			})
		});
	}
	if (!("autofocus" in document.createElement("INPUT")))
	{
		$("[autofocus]").first().focus();
	}
	
	// Typeahead
	$("BODY").on("keyup", "*[typeahead]", typeaheadKeyUp)
			 .on("keydown", "*[typeahead]", typeaheadKeyDown)
			 .on("focus", "*[typeahead]", typeaheadShow)
			 .on("blur", "*[typeahead]", typeaheadHide);
		
	$(".DaysOfMonthChooser TD").on("click", daysOfMonthChooser);
	
	// Hack to hide the fixed-positioned tab bar on iPhone
	if (UserAgent.AppleTouch && UserAgent.SmartPhone)
	{
		$("BODY").on("focus", "INPUT:not([type=checkbox]):not([type=radio]):not([type=submit]), SELECT, TEXTAREA", function()
		{
			$(".TopBar .Tabs TABLE").hide();
			$(".Fixed").css("position", "static");
		})
		.on("blur", "INPUT:not([type=checkbox]):not([type=radio]):not([type=submit]), SELECT, TEXTAREA", function()
		{
			$(".TopBar .Tabs TABLE").show();
			$(".Fixed").css("position", "fixed");
		});
//		// Hide the address bar
//		setTimeout(function(){
//			window.scrollTo(0, window.pageYOffset + 1);
//			$('body').append($('<div></div>').attr('id','iOsScrollFix').css('height','101%').css('overflow','hidden'));
//			setTimeout(function() {
//			  $('#iOsScrollFix').remove();
//			},0);
//		},500);
	}
	if (UserAgent.AppleTouch)
	{
		// Open links in same window on iPhone
		$("A[href]").on("click", function(ev){
			var href = $(this).attr("href");
			var target = $(this).attr("target");
			if (!href.indexOf("javascript:")==0 && !target)
			{
				ev.preventDefault();
				window.location = href;
			}
		});
	}
	
//	// screenshot support
//	$("H1").click(function(ev)
//		{
//			$(".TopBar .Tabs TABLE").toggle();
//			ev.preventDefault();
//		});
});


//$(window).load(function()
//{
//	if (UserAgent.iPhone || UserAgent.iPod)
//	{
//		// Hide the address bar on iPhone
//		window.setTimeout(function(){window.scrollTo(0, 1);$(window).scroll();}, 10);
//	}
//});

//function neatCapsInput(ev)
//{
//	var t=$(ev.target);
//	if (!t.attr('neatcaps') || t.attr('neatcaps').toLowerCase()!=t.val().toLowerCase())
//	{
//		var v=t.val();
//		var vv="";
//		for (var i=0;i<v.length;i++)
//		{
//			if (i==0 || v[i-1]==" " || v[i-1]=="\'" || v[i-1]==".")
//			{
//				vv+=v[i].toUpperCase();
//			}
//			else
//			{
//				vv+=v[i].toLowerCase();
//			}
//		}
//		t.val(vv).attr("neatcaps", vv);
//	}
//}

// Typeahead
function typeaheadKeyDown(ev)
{
	var $inputBox = $(ev.target);
	$inputBox.attr("preval", $inputBox.val());
	
	if (ev.which==13 || ev.which==9) // Enter
	{
		var $hover = $inputBox.next().children(".Hover");
		if ($hover.length!=0)
		{
			$inputBox.val($hover.attr("value")).attr("preval", $hover.attr("value")).focus();
			$inputBox.prev().val($hover.attr("key"));				
			if (ev.which==13)
			{
				// Don't submit form
				ev.preventDefault();
			}
		}
	}	
}

function typeaheadKeyUp(ev)
{
	// Clear the key field and populate dropdown if the value changed
	var $inputBox = $(ev.target);
	if ($inputBox.val()!=$inputBox.attr("preval"))
	{
		$inputBox.prev().val("");
		typeaheadShow(ev);
	}

	$div = $inputBox.next();
	if (ev.which==40) // arrow down
	{
		var $hover = $div.children(".Hover").removeClass("Hover").next().addClass("Hover");
		if ($hover.length==0)
		{
			$div.children().first().addClass("Hover");
		}
	}
	else if (ev.which==38) // arrow up
	{
		var $hover = $div.children(".Hover").removeClass("Hover").prev().addClass("Hover");
		if ($hover.length==0)
		{
			$div.children().last().addClass("Hover");
		}
	}
}

function typeaheadShow(ev)
{
	var $inputBox = $(ev.target);
	var val = jQuery.trim($inputBox.val());
	if (val.length==0)
	{
		$("#typeAheadPopup_"+$inputBox.attr("typeAheadIndex")).hide();
		return;
	}
	
	$.ajax(
		$inputBox.attr("typeahead"),
		{
			data: "q=" + encodeURIComponent($inputBox.val()),
			dataType: "json",
			success:
				function(data, textStatus, jqXHR)
				{
					showAutoComplete($inputBox, data);
				}
		}
	);
}

function typeaheadHide(ev)
{
	var $inputBox = $(ev.target);
	$("#typeAheadPopup_"+$inputBox.attr("typeAheadIndex")).delay(50).hide(0);
}

var typeAheadIndex = 0;
function showAutoComplete($inputBox, data)
{
	if (!$inputBox.attr("typeAheadIndex"))
	{
		$inputBox.attr("typeAheadIndex", typeAheadIndex);
		typeAheadIndex++;
	}

	var $div = $("#typeAheadPopup_"+$inputBox.attr("typeAheadIndex"));
	if ($div.length==0)
	{
		$div = $("<div></div>")
				.attr("id", "typeAheadPopup_"+$inputBox.attr("typeAheadIndex"))
				.addClass("TypeAheadPopup")
				.css("top", ($inputBox.offset().top + $inputBox.outerHeight() - 1) + "px")
				.css("left", ($inputBox.offset().left) + "px")
				.css("min-width", ($inputBox.outerWidth()) + "px")
				.css("max-width", "400px")
				.insertAfter($inputBox);
	
	}
	
	$div.hide().children().remove();
	
	if (data.options.length==0)
	{
		// $inputBox.prev().val("");
		return;
	}
	if (data.options.length==1 && jQuery.trim(data.options[0].value.toLowerCase())==jQuery.trim($inputBox.val().toLowerCase()))
	{
		$inputBox.prev().val(data.options[0].key);
		return;
	}
	
	for (var i=0; i<data.options.length; i++)
	{
		var k = data.options[i].key;
		var v = data.options[i].value;
		var h = data.options[i].html;
		if (h==null || h.length==0)
		{
			h = v;
		}
		if (k==null || k.length==0)
		{
			k = v;
		}
		
		var $option = $("<div></div>")
			.attr("value", v)
			.attr("key", k)
			.append($("<div>" + h + "</div>"))
			.on("mousedown",
				function(ev)
				{
					var $x = $(ev.target).parents("DIV[value]").first();
					$inputBox.val($x.attr("value")).attr("preval", $x.attr("value")).focus();
					$inputBox.prev().val($x.attr("key"));				
					ev.preventDefault();
				})
			.on("mouseenter",
				function(ev)
				{
					$(ev.target).addClass("Hover").siblings().removeClass("Hover");
				});
		$div.append($option);
	}
	$div.show();
}

// Control array
function ctrlArrAddRow(target)
{
	var $elem = $(target).parents("TABLE").first();
	var $size = $elem.prev();
	var $add = $elem.find("TR").last().prev();
	var dummyIndex = $add.attr("new");
	var $clone = $add.clone();
		
	$clone.find("*[name]").each(function(index) {
		$(this).attr("name", $(this).attr("name").replace(dummyIndex, $size.val()));
	});

	$clone
		.insertBefore($add)
		.css("display", "block") // For IE7 that doesn't support display:table-row
		.css("display", "table-row")
		.find("INPUT,SELECT,TEXTAREA").first().focus();

	$size.val(1 + Number($size.val()));
}
function ctrlArrRemoveRow(target)
{
	$(target).parents("TR").first().remove();	
}

// View table
function vuTblScroll(name, index)
{
	var $cursor = $('INPUT[name='+name+']');
	var $form = $cursor.parents('FORM').first();
	$form[0].reset();
	$cursor.val(index);
	$form.find('INPUT[type=submit]').first().click();
}

// Days of month chooser
function daysOfMonthChooser(ev)
{
	var $target = $(ev.target);	
	if ($target.hasClass("Disabled")) return;
	var $table = $target.parents("TABLE").first();
	if ($table.hasClass("Enabled")==false) return;
	
	if ($target.hasClass("Month")) // Header
	{
		var $selected = $table.find("TD.Enabled.Selected");
		var hasSelected = $selected.length>0;
		
		$table.find("TD.Day.Enabled").each(function (index) {
			if (hasSelected==false)
			{
				$(this).addClass("Selected");
			}
			else
			{
				$(this).removeClass("Selected");
			}
		});
	}
	else if ($target.hasClass("DOW")) // Day of week
	{
		var index = $target.index();
		var $selected = $table.find("TD.Enabled.Selected:nth-child("+(index+1)+")");
		var hasSelected = $selected.length>0;
		
		$table.find("TD.Day.Enabled:nth-child("+(index+1)+")").each(function (index) {
			if (hasSelected==false)
			{
				$(this).addClass("Selected");
			}
			else
			{
				$(this).removeClass("Selected");
			}
		});
	}
	else if ($target.hasClass("Day") && $target.hasClass("Enabled")) // Day
	{
		$target.toggleClass("Selected");
	}
	
	var bitset = "";
	$table.find("TD.Day").each(function(index) {
		var $this = $(this);
		bitset += ($this.hasClass("Selected") ? "1" : "0");
	});
	$table.prev().val(bitset);
}

// Image uploader
function imgUploadClear(name)
{
	$("INPUT[name="+name+"]").val("")
	.closest("TABLE").addClass("StateEmpty").removeClass("StateCurrent").removeClass("StateNew").removeClass("StateUploaded")
	.prev().val("empty");
}
function imgUploadUndo(name)
{
	$("INPUT[name="+name+"]").val("")
	.closest("TABLE").removeClass("StateEmpty").addClass("StateCurrent").removeClass("StateNew").removeClass("StateUploaded")
	.prev().val("current");
}
function imgUploadNew(name, click)
{
	var input = $("INPUT[name="+name+"]").one("change", imgUploadSelected);
	if (click) input.click();
}
function imgUploadSelected(ev)
{
	var $target = $(ev.target);
	var name = $target.attr("name");
	var val = $target.val();
	if (val!="")
	{
		var p = val.lastIndexOf("\\");
		if (p>=0)
		{
			val = val.substring(p+1);
		}
		p = val.lastIndexOf("/");
		if (p>=0)
		{
			val = val.substring(p+1);
		}
		$target.closest("TABLE").removeClass("StateEmpty").removeClass("StateCurrent").addClass("StateNew").removeClass("StateUploaded")
			.prev().val("new").next()
			.find(".Descs > .New").html(val);
	}
}

// Tooltip
function tooltipToggle(e)
{
	$(e).toggleClass("Hover");
}

// Back button
function backStackSize()
{
	var sz = windowStorage.getItem("bkStkSz");
	if (sz==null)
	{
		return 0;
	}
	else
	{
		return Number(sz);
	}
}
function backPush()
{
	var sz = backStackSize();
	if (sz==0 || windowStorage.getItem("bkStkID"+(sz-1))!=window.location.pathname)
	{
		windowStorage.setItem("bkStkU"+sz, window.location.href);
		windowStorage.setItem("bkStkID"+sz, window.location.pathname);
		windowStorage.setItem("bkStkSz", (sz+1));
	}
	else
	{
		windowStorage.setItem("bkStkU"+(sz-1), window.location.href);
	}	
}
function backActivateButton(backBtnID, menuBtnID)
{
	var sz = backStackSize();
	if (sz>1)
	{
		$("#"+backBtnID).show().on("click", backPopAndRedirect);
	}
	else
	{
		$("#"+menuBtnID).show().on("click", backRevealMenu);
	}
}
var backMenuVisible = false;
function backRevealMenu(ev)
{
	if (backMenuVisible==false)
	{
		$('#page').toggle();
		$('#hotButtons').css('visibility','hidden');
		$('#navbar').slideToggle(200);
	}
	else
	{
		$('#navbar').toggle();
		$('#page').toggle();
		$('#hotButtons').css('visibility','visible');
	}
	backMenuVisible = !backMenuVisible;
}
function backPopAndRedirect(ev)
{
	var sz = backStackSize();
	if (sz>1)
	{
		var url = windowStorage.getItem("bkStkU"+(sz-2));
		windowStorage.setItem("bkStkSz", (sz-2));
		window.location.href = url;
		if (ev!=null)
		{
			ev.preventDefault();
		}
	}
}
function backClear()
{
	windowStorage.setItem("bkStkSz", 0);
}
function backPrint()
{
	var sz = backStackSize();
	var i;
	for (i=0; i<sz; i++)
	{
		document.write(windowStorage.getItem("bkStkU"+i));
		document.write("<br>");
	}
}
backPush();
