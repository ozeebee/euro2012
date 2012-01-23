$(function() {
	// restrict input fields to numeric input only
	$("input").keydown(function (event) {
		var key = event.which || event.keyCode;
		var char = String.fromCharCode(key);
		if (/^[0-9]+$/.test(char))
			return true;
		return false;
	});
	
	// add listener for forecast text inputs
	$("input").keypress(function (event) {
		// show save button
		var btn = $("a.saveBtn", $(this).parent());
		if (btn.hasClass("notDisplayed"))
			btn.removeClass("notDisplayed");
		// remove notif icon (if any)
		$("span.notif", btn.parent()).remove();
	});
});


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
