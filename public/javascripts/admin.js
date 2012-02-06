function setCurrentDateTime() {
	console.log("setCurrentDateTime()");
	var dateStr = $("#currentDateChooser").val();
	var timeStr = $("#currentTime").val();
	var dateTimeStr = dateStr + " " + timeStr;
	console.log("  posting dateTime " + dateTimeStr);
	$.post("/admin/currentDateTime",
			{dateTime: dateTimeStr},
			function(data) {
				console.log("Ok ! currentDateTime updated");
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
	});
}

function updateScore(matchId, url) {
	console.log("updateScore() matchId = " + matchId);
	var div = $("#score_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);
	var data = {
		scoreA: scoreAinput.val(), 
		scoreB: scoreBinput.val()
	};
	
	$.post("/admin/matches/"+matchId+"/result", 
			data,
			function(data) { // Success function
				// remove error markers (if any)
				scoreAinput.removeClass("error");
				scoreBinput.removeClass("error");
				// show ok notif icon
				$("img", div).prop("src", assetsRoot + "images/button_ok.png").removeClass("invisible");
			}
	).fail(function(jqXHR) { // Error function
		if (jqXHR.status == 400) {
			var jsonStr = jqXHR.responseText;
			var errorFields = $.parseJSON(jsonStr);
			var fieldToFocus = null;
			// remove error markers (if any)
			scoreAinput.removeClass("error");
			scoreBinput.removeClass("error");
			// add error markers
			for (var i=0; i<errorFields.length; i++) {
				var field = errorFields[i];
				if (field == "scoreA") { 
					scoreAinput.addClass("error");
					fieldToFocus = scoreAinput;
				}
				else if (field == "scoreB") {
					scoreBinput.addClass("error");
					if (fieldToFocus == null)
						fieldToFocus = scoreBinput;
				}
			}
			// focus field
			fieldToFocus.select();
			fieldToFocus.focus();
		}
		else {
			console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		}
	});
}

function clearScore(matchId, url) {
	console.log("clearScore()");
	var div = $("#score_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);

	var data = {};
	$.ajax({
		type: 'DELETE',
		url: url,
		data: data,
		success: function(data) { // Success function
			console.log("clear success. data=" + data);
			// empty fields
			scoreAinput.val("");
			scoreBinput.val("");
		}
	}).fail(function(jqXHR) { // Error function
		console.log("Clear error !!!");
		console.log("  status = " + jqXHR.status);
		console.log("  response = " + jqXHR.responseText);
	});
}

// ===================== User Forecasts =======================================

function showUserForecasts(username) {
	console.log("showUserForecasts");
	
	var url = userForecastsUrl.replace(/:username/, username);
	console.log("  url = " + url);
	$.get(url,
			function(data) {
				console.log("Ok ! got data");
				$("#userForecasts").html(data);
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
	});
}

function deleteUserForecasts(event, url) {
	console.log("deleteUserForecasts");
	var btn = $(event.target);
	btn.button('loading');
	var data = {};
	$.ajax({
		type: 'DELETE',
		url: url,
		data: data,
		success: function(data) { // Success function
			console.log("Ok ! got data");
			btn.button('reset');
			$("#userForecasts").html(data);
		}
	}).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		btn.button('reset');
	});
}

function generateRandomForecasts(event, url) {
	console.log("generateRandomForecasts");
	var btn = $(event.target);
	btn.button('loading');
	$.get(url,
			function(data) {
				console.log("Ok ! got data");
				btn.button('reset');
				$("#userForecasts").html(data);
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		btn.button('reset');
	});
}

function updateForecast(username, matchId, url) {
	console.log("updateForecast");
	
	var div = $("#forecast_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);
	var data = {
		username: username,
		matchId: matchId,
		scoreA: scoreAinput.val(), 
		scoreB: scoreBinput.val()
	};

	$.post(url, 
			data,
			function(data) { // Success function
				// show ok notif icon
				$("img", div).prop("src", assetsRoot + "images/checkmark.gif").removeClass("invisible");
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		// show error notif icon
		$("img", div).prop("src", assetsRoot + "images/x-red.gif").removeClass("invisible");
	});
}

function deleteForecast(username, matchId, url) {
	console.log("deleteForecast");
	var div = $("#forecast_" + matchId);
	$.ajax({
		type: 'DELETE',
		url: url,
		success: function(data) { // Success function
			// show ok notif icon
			$("img", div).prop("src", assetsRoot + "images/checkmark.gif").removeClass("invisible");
			// empty text fields
			var scoreAinput = $("input:eq(0)", div);
			var scoreBinput = $("input:eq(1)", div);
			scoreAinput.val("");
			scoreBinput.val("");
		}
	}).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		// show error notif icon
		$("img", div).prop("src", assetsRoot + "images/x-red.gif").removeClass("invisible");
	});	
}

$(function() {
	$("#form").submit(function () {
		var zdate = $("#datepicker").prop("value");
		var hour = $("#kickoffHour").val();
		$("#kickoff").prop("value", zdate + " " + hour);
		console.log("kickoff = " + $("#kickoff").prop("value"))
		return true;
	});

	// initialize currentDateTimeChooser date picker
	$("#currentDateChooser").datepicker({
		dateFormat: "dd/mm/yy",
		changeMonth: true,
		changeYear: true,
		defaultDate: "08/06/2012"
		//showOn: "button",
		//buttonImage: "@routes.Assets.at("images/calendar-blue.png")",
		//buttonImageOnly: true,
		/*
		onSelect: function(dateText, inst) {
			//console.log("dateText = " + dateText)
			var hour = $("#kickoffHour").val();
			$("#datepicker").prop("value", dateText + " " + hour);
			return false;
		}*/
	});
});