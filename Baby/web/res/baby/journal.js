$(function() {
	var $EntryPlaceholder = $('#EntryPlaceholder');
	var $EntryInputs = $('#EntryInputs');
	
	function expandNewEntryPanel() {
		$EntryPlaceholder.hide();
		$EntryInputs.show('fast', function() {
			var $textArea = $('TEXTAREA', this);
			if ($textArea.length > 0) {
				$textArea[0].focus();
			}
		});
	}
	
	$EntryPlaceholder.one('click', expandNewEntryPanel);
	
	if ($EntryInputs.hasClass('Show')) {
		expandNewEntryPanel();
	}
	
	var $Edit = $('#Edit');
	
	if ($Edit.length > 0) {
		var phoneEditMode = false;
		var $HotButton = $('#hotButtons INPUT[type="button"]');
		
		function togglePhoneEditMode() {
			$HotButton.val($Edit.attr(phoneEditMode ? 'labeledit' : 'labelcancel'));
			$('.EditButton').toggleClass('Show');
			
			phoneEditMode = !phoneEditMode;
		}
		
		$Edit.on('click', function(e) {
			e.preventDefault();
			togglePhoneEditMode();
		});
	}
});