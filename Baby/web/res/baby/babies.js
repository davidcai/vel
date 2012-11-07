$(function() {
	$('#BabyNumber SELECT').on('change', function() {
	    var count = $(this).val();
	    
	    $('#BabyList LI').each(function(index) {
	    	var $li = $(this);
	    	if (index < count) {
	    		$li.show();
	    	}
	    	else
    		{
	    		$li.hide();
	    	}
	    });
	});
});