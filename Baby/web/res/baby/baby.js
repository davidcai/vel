var timelineDragged = null;
$(document).ready(function()
{
    $(".Slider")
    .on("mousedown touchstart", "IMG", function(ev)
    {
        timelineDragged = $(ev.target).closest("TD");
        ev.preventDefault();
    })
    .on("mousemove", ".Stop", function(ev)
    {
        if (timelineDragged!=null)
        {
            $(ev.target).children("IMG").removeClass("Hide");
            $(ev.target).siblings().children("IMG").addClass("Hide");
        }
    })
    .on("touchmove", ".Stop", function(ev)
    {
        if (timelineDragged!=null)
        {
        	var touch = ev.originalEvent.touches[0] || ev.originalEvent.changedTouches[0];
        	var element = document.elementFromPoint(touch.pageX, timelineDragged.offset().top);
        	 
            $(element).closest(".Stop").children("IMG").removeClass("Hide");
            $(element).closest(".Stop").siblings().children("IMG").addClass("Hide");
        }
    })
    .on("mouseleave", "TR.Bar", function(ev)
    {
        if (timelineDragged!=null)
        {
            timelineDragged.children("IMG").removeClass("Hide");
            timelineDragged.siblings().children("IMG").addClass("Hide");
        }
    })
    .on("mouseup", ".Stop", function(ev)
    {
        var clicked = $(ev.target).closest("TD");
        if (timelineDragged!=null && timelineDragged[0]!=clicked[0])
        {
            window.location.href = clicked.attr("href");
        }
    })
    .on("touchend", ".Stop", function(ev)
    {
    	if (timelineDragged!=null)
    	{
	        var touch = ev.originalEvent.touches[0] || ev.originalEvent.changedTouches[0];
	        var element = document.elementFromPoint(touch.pageX, timelineDragged.offset().top);
	        var clicked = $(element).closest("TD");
	        if (timelineDragged[0]!=clicked[0])
	        {
	            window.location.href = clicked.attr("href");
	        }
	    }
    });
    $(document).on("mouseup touchend touchcancel", function(ev)
    {
        timelineDragged = null;
    });
});
