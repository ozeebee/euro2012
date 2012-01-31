$(function() {
	// restrict input fields to numeric input only
	$("input").keydown(function (event) {
		var key = event.which || event.keyCode;
		 // Allow: backspace, delete, tab and escape
        if (key == 46 || key == 8 || key == 9 || key == 27 || 
             // Allow: Ctrl+A
            (key == 65 && event.ctrlKey === true) || 
             // Allow: home, end, left, right
            (key >= 35 && key <= 39)) {
                 // let it happen, don't do anything
                 return true;
        }
        // check numeric value
		var char = String.fromCharCode(key);
		if (/^[0-9]+$/.test(char))
			return true;
		return false;
	});
	
	// add listener for forecast text inputs
	$("input").keypress(function (event) {
		// show save button
		var btn = $("a.saveBtn", $(this).parent());
		showSaveButton(btn);
	});
});

function showSaveButton(btn) {
	if (btn.hasClass("notDisplayed"))
		btn.removeClass("notDisplayed");
	// remove notif icon (if any)
	$("span.notif", btn.parent()).remove();
}

function saveForecast(matchId, url) {
	console.log("saveForecast");
	
	var div = $("#forecast_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);
	var data = {
		matchId: matchId,
		scoreA: scoreAinput.val(), 
		scoreB: scoreBinput.val()
	};

	// show progress
	var btn = $("a.saveBtn", div);
	btn.addClass("disabled");
	$("span", btn).text("saving...");
	
	var asyncOpFinished = function() {
		// remove progress
		$("span", btn).text("save");
		btn.removeClass("disabled");
		// hide save button
		btn.addClass("notDisplayed");
	};
	
	$.post(url, 
			data,
			function(data) { // Success function
				asyncOpFinished();
				// show ok notif icon
				btn.parent().append(getResultIndicator(true));
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		asyncOpFinished();
		// show error notif icon
		btn.parent().append(getResultIndicator(false));
	});
}

function getResultIndicator(isSuccess) {
	var img = $('<img style="padding-right:5px;">');
	img.prop("src", assetsRoot + "images/" + (isSuccess ? "checkmark.gif" : "x-red.gif"));
	var span = $('<span class="notif"/>');
	span.append(img);
	span.append(isSuccess ? "saved" : "error");
	return span;
}

function showModifiableForecasts(event) {
	var checkbox = event.target;

	// select all elements that does not have input beneath
	var elems = $("div.dateBlock:not(:has(input))").addClass("notDisplayed");

	if (checkbox.checked) {
		// hide matches that cannot be forecast
		elems.addClass("notDisplayed");
	}
	else {
		// show all
		elems.removeClass("notDisplayed");
	}
}

function randomForecast(matchId) {
	var div = $("#forecast_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);
	scoreAinput.val(randomScore());
	scoreBinput.val(randomScore());
	
	showSaveButton($("a.saveBtn", div));
}

function randomScore() {
	/*
	lazy val randomScoreRanges = Array(
			0  until 30, // score 0
			30 until 60, // score 1
			60 until 80, // score 2
			80 until 90, // score 3
			90 until 95, // score 4
			95 until 98, // score 5
			98 until 100 // score 6
		) 	 
	 */
	var scoreProbArray = [30, 60, 80, 90, 95, 98, 100];
	var randVal = Math.random()
	var randInt = Math.floor(randVal * 100);
	//console.log("  randVal = " + randVal + " randInt = " + randInt);
	var score = -1;
	for (var i=0; i<scoreProbArray.length; i++) {
		var lowerBound = (i == 0 ? 0 : scoreProbArray[i-1]);
		var upperBound = scoreProbArray[i];
		//console.log("   testing range [" + lowerBound + "," + upperBound + "]");
		if (lowerBound <= randInt && randInt < upperBound) {
			//console.log("    ok score = " + i);
			score = i;
			break;
		}
	}
	if (score == -1) {
		//console.log("    could not determine score !!")
		score = 0;
	}
	//console.log("    score = " + score);
	return score;
}
