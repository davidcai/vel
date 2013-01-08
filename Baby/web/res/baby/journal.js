$(function() {
	var $NewEntryPlaceHolder = $('#NewJournalEntryPlaceHolder');
	var $NewEntryPanel = $('#NewJournalEntryPanel');
	
	function expandNewEntryPanel() {
		$NewEntryPlaceHolder.hide();
		$NewEntryPanel.addClass('Expanded').find('TEXTAREA').focus();
	}
	
	$NewEntryPlaceHolder.one('click', function(e) {
		expandNewEntryPanel();
	});
	
	if ($NewEntryPanel.hasClass('Expanded')) {
		console.log('************* ');
		expandNewEntryPanel();
	}
});