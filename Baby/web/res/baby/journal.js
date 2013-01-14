$(function() {
	var $EntryPlaceholder = $('#EntryPlaceHolder');
	var $EntryInputs = $('#EntryInputs');
	
	function expandNewEntryPanel() {
		$EntryPlaceholder.hide();
		$EntryInputs.show('slow', function() {
			$(this).find('TEXTAREA').focus();
		});
	}
	
	$EntryPlaceholder.one('click', expandNewEntryPanel);
	
	if ($EntryInputs.hasClass('Show')) {
		expandNewEntryPanel();
	}
});