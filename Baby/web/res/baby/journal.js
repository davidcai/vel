$(function() {
	var $EntryPlaceholder = $('#EntryPlaceholder');
	var $EntryInputs = $('#EntryInputs');
	
	function expandNewEntryPanel() {
		$EntryPlaceholder.hide();
		$EntryInputs.show('fast', function() {
			var $textFields = $('INPUT[type="text"]', this);
			if ($textFields.length > 0) {
				$textFields[0].focus();
			}
			else {
				var $textAreas = $('TEXTAREA', this);
				if ($textAreas.length > 0) {
					$textAreas[0].focus();
				}
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