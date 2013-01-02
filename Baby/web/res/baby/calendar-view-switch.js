$(function() {
	var $viewSwitch = $('#CalendarViewSwitch');
	var $viewContainer = $('#CalendarViewContainer');
	var $viewFlag = $('INPUT[name="view"]', $viewSwitch);
	
	function switchView(viewId) {
		$('.View', $viewContainer).each(function() {
			var $this = $(this);
			if ($this.attr('id') == viewId) {
				$this.show();
			}
			else {
				$this.hide();
			}
		});
		
		$('A', $viewSwitch).each(function() {
			var $this = $(this);
			if ($this.attr('for') == viewId) {
				$this.addClass('CalendarViewSelected');
			}
			else {
				$this.removeClass('CalendarViewSelected');
			}
		});
		
		$viewFlag.val(viewId);
	}
	
	switchView($viewFlag.val());
	
	$('A', $viewSwitch).on('click', function(e) {
		switchView($(this).attr('for'));
	});	
});
