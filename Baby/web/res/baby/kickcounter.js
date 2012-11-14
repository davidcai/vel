$(function() {

	var $lblCountdown = $('#Countdown');
	var $lblKicks = $('#Kicks SPAN');
	var $btnStart = $('INPUT[name="start"]');
	var $btnStop = $('INPUT[name="stop"]');
	var $btnCount = $('INPUT[name="count"]');
	var $btnSave = $('INPUT[name="save"]');
	
	var model = {
		time: 60, 
		kicks: 0
	};
	
	var intervalID;
	
	// Button click events
	$btnStart.on('click', start);
	$btnStop.on('click', stop);
	$btnCount.on('click', count);
	
	// Start
	function start(e) {
		if (e) {
			e.preventDefault();
		}
		
		$btnStop.show();
		$btnCount.show();
		$btnStart.hide();
		$btnSave.hide();

		setTime(5);
		setKicks(0);
		
		if (intervalID) {
			clearInterval(intervalID);
		}
		
		intervalID = setInterval(function() {
			setTime(--model.time);
		}, 1000);
	}
	
	// Stop
	function stop(e) {
		if (e) {
			e.preventDefault();
		}
		
		$btnStart.show();
		$btnSave.show();
		$btnStop.hide();
		$btnCount.hide();

		if (intervalID) {
			clearInterval(intervalID);
		}
	}
	
	// Count
	function count(e) {
		if (e) {
			e.preventDefault();
		}
		
		$lblKicks.text(++model.kicks);
	}
	
	function setTime(time) {
		if (time >= 0) {
			model.time = time;
			
			if (time == 60) {
				$lblCountdown.text('1:00');
			}
			else if (time > 9) {
				$lblCountdown.text('0:' + time);
			}
			else {
				$lblCountdown.text('0:0' + time);
			}
		}
		
		if (time == 0) {
			zero();
		}
	}
	
	function zero() {
		stop();
	}
	
	function setKicks(kicks) {
		model.kicks = kicks;
		$lblKicks.text(kicks);
	}
});