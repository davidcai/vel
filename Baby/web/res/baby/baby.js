function initBabyNumberChangeListener(defaultBabyName) {
	$('#BabyNumber SELECT').on('change', function() {
	    var $list = $('#BabyList LI');
	    var oldCount = $list.length;
	    var newCount = $(this).val();
	
	    for (var i = oldCount; i < newCount; i++) {
	        var $newItem = $list.eq(0).clone();
	        
	        $('INPUT[type=text]', $newItem).attr({
	        	name: 'name_' + i, 
	        	value: ''
	        }).removeClass("Error");
	        $('SELECT', $newItem).attr('name', 'gender_' + i);
	        $('INPUT[type=hidden]', $newItem).attr({
	        	name: 'id_' + i, 
	        	value: ''
	        });
	        
	        $('#BabyList').append($newItem);
	    }
	    
	    for (var i = oldCount - 1; i >= newCount; i--) {
	        $list.eq(i).remove();
	    }
	});
}
