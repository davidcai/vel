$(function() {
	$('#NewJournalEntryPlaceHolder').one('click', function(e) {
		$(this).hide();
		$('#NewJournalEntryPanel').addClass('Expanded').find('TEXTAREA').focus();
	});
});