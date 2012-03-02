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
	
	// penalties ?
	var pendiv = $("#penscore_" + matchId);
	if (pendiv.length > 0) {
		var penScoreAinput = $("input:eq(0)", pendiv);
		var penScoreBinput = $("input:eq(1)", pendiv);
		var penScoreA = penScoreAinput.val();
		var penScoreB = penScoreBinput.val();
		//console.log("penScoreA=["+penScoreA+"] penScoreB=["+penScoreB+"]");
		if (penScoreA.length == 0 && penScoreB.length == 0) {
			// no penalty
		} else if (penScoreA.length != 0 && penScoreB.length == 0) {
			// only one of the penalty score is set ! error
			penScoreBinput.addClass("error");
			penScoreBinput.select();
			penScoreBinput.focus();
			return;
		} else if (penScoreA.length == 0 && penScoreB.length != 0) { 
			// only one of the penalty score is set ! error 
			penScoreAinput.addClass("error");
			penScoreAinput.select();
			penScoreAinput.focus();
			return;
		} else {
			// penalties provided
			data.penaltyScoreA = penScoreA;
			data.penaltyScoreB = penScoreB;
		}
	}
	
	$.post("/admin/matches/"+matchId+"/result", 
			data,
			function(data) { // Success function
				// show ok notif icon
				$("img", div).prop("src", assetsRoot + "images/button_ok.png").removeClass("invisible");
			}
	).fail(function(jqXHR) { // Error function
		if (jqXHR.status == 400) {
			var jsonStr = jqXHR.responseText;
			var errorFields = $.parseJSON(jsonStr);
			var fieldToFocus = null;
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
	}).complete(function () {
		// remove error markers (if any)
		scoreAinput.removeClass("error");
		scoreBinput.removeClass("error");
		penScoreAinput.removeClass("error");
		penScoreBinput.removeClass("error");
	});
}

function clearScore(matchId, url) {
	console.log("clearScore()");
	var div = $("#score_" + matchId);
	var scoreAinput = $("input:eq(0)", div);
	var scoreBinput = $("input:eq(1)", div);
	var pendiv = $("#penscore_" + matchId);
	var penScoreAinput = $("input:eq(0)", pendiv);
	var penScoreBinput = $("input:eq(1)", pendiv);

	var data = {};
	$.ajax({
		type: 'DELETE',
		url: url,
		data: data,
		success: function(data) { // Success function
			console.log("clear success. data=" + data);
			// empty fields
			scoreAinput.val("");
			scoreBinput.val("")
			penScoreAinput.val("");
			penScoreBinput.val("");
		}
	}).fail(function(jqXHR) { // Error function
		console.log("Clear error !!!");
		console.log("  status = " + jqXHR.status);
		console.log("  response = " + jqXHR.responseText);
	});
}

/**
 * Used for selecting a team or formula in non-group stage matches
 * (require jsonGetTeamsUrl and setTeamNameUrl variables to be set)
 */
function selectTeam(event, formula, matchId, isTeamA) {
	event.preventDefault();
	var $elem = $(event.target);
	
	$.getJSON(jsonGetTeamsUrl, function (data) {
		var teams = data;
		//console.log("teams = ", teams);
		
		var $content = $('<p><select autofocus required><option value="'+formula+'" selected>'+formula+'</option></select></p>');
		var $select = $("select", $content);
		$.each(teams, function (i, team) {
			$('<option/>').val(team).text(team).appendTo($select);
		});
		
		var okfunc = function(event) {
			var $dialog = event.data.dialog; // passed as data event to the handler
			var selectedTeam = $("select", $dialog).val();
			//console.log("selectedTeam = " + selectedTeam, $dialog);
			data = {
				matchId: matchId,
				team: selectedTeam,
				isTeamA: isTeamA
			};
			
			$.post(setTeamNameUrl,
					data,
					function(data) {
						console.log("Ok ! data received " + data);
						// set new team name
						$elem.text(selectedTeam);
					}
			).fail(function(jqXHR) { // Error function
				console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
			}).complete(function() {
				$dialog.remove();
			});
		};
		
		////////////////////////////////////
		// popover creation 
		
		// remove any existing popover before
		$("div.popover").remove();
		
		var title = "Choose Team";
		var dialogContent = $content.html();
		dialogContent += '<p style="text-align: right;"><button id="btn-ok" class="btn btn-small btn-primary">Ok</button>&nbsp;<button id="btn-cancel" class="btn btn-small">Cancel</button></p>';
		
		var $dialog = $('<div class="popover"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"></div></div></div>');
		$dialog.appendTo(document.body);
		$dialog.addClass("right");
		$dialog.find("h3").text(title);
		$dialog.find("div.popover-content").html(dialogContent);
		$dialog.find("button#btn-ok").click({dialog:$dialog}, okfunc);
		$dialog.find("button#btn-cancel").click(function() {
			$dialog.remove();
		});
		/*$dialog.bind('mouseleave', function() {
			$dialog.fadeOut('fast', function() {
				console.log("removing popover...");
				$dialog.remove();
			});
		});*/
	
		var x = $elem.offset().left + $elem[0].offsetWidth;
		//console.log("elem.top=" + $elem.offset().top + ", elem.offsetHeight=" + $elem[0].offsetHeight + ", dialog.offsetHeight=" + $dialog.outerHeight());
		var y = $elem.offset().top + ($elem[0].offsetHeight/2) - ($dialog.outerHeight()/2); // we use outerHeight instead of offsetHeight because offsetHeight is reported as zero during popover costruction
		
		$dialog.css({ top: y, left: x, display: 'block' });
	})
	.error(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
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

//===================== Scenarios =============================================

function showScenarioCategory(event, category) {
	var link = $(event.target);
	var categoryTable = $("table#cat_"+category);
	
	var activeLink = $("ul.nav-list li.active");
	activeLink.removeClass("active");
	link.parent().addClass("active");
	
	$("#scenario_content table.show").removeClass("show").addClass("hide");
	categoryTable.addClass("show");
	
	event.preventDefault();
	return false;
}

function applyScenario(event, url) {
	console.log("applyScenario url=" + url);
	var btn = $(event.target);
	btn.button('loading');
	cleanScenarioNotif();
	$.post(url, 
			function(data) { // Success function
				btn.button('reset');
			}
	).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		btn.button('reset');
		notifyScenario("Scenario apply error", jqXHR.responseText, true);
	});
}

function unapplyScenario(event, url) {
	console.log("unapplyScenario url=" + url);
	var btn = $(event.target);
	btn.button('loading');
	cleanScenarioNotif();
	$.ajax({
		type: 'DELETE',
		url: url, 
		success: function(data) { // Success function
				btn.button('reset');
			}
	}).fail(function(jqXHR) { // Error function
		console.log("AJAX Post error !! " + jqXHR.status + " " + jqXHR.responseText);
		btn.button('reset');
		notifyScenario("Scenario apply error", jqXHR.responseText, true);
	});
}

function cleanScenarioNotif() {
	$("#scenario_notif").attr("class", "hide");
}
function notifyScenario(title, message, isError) {
	var classes = "alert alert-block " + (isError ? "alert-error" : "alert-success");
	var notif = $("#scenario_notif");
	notif.html("<h4 class=\"alert-heading\">" + title + "</h4> " + message);
	notif.attr("class", classes);
}

//===================== Initializations =======================================

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
