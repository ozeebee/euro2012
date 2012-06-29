/**
 * ozeebee scripts 
 */

/**
 * Show the content for the given (bootstrap) tab.
 * The Tab content can be either static (already present in the DOM) or
 * dynamic in which case a url must be passed.
 * 
 * REQUIRES : variable assetsRoot to be set to the root url for assets (ex: var assetsRoot = "/assets/");
 */
function showTab(event, url, reload) {
	event.preventDefault();
	var link = $(event.target);
	var divId = link.attr("href");
	console.log("showing tab with div id " + divId);

	var li = link.parent("li");
	if (li.hasClass("active"))
		return; // tab already selected
	
	if (reload == undefined)
		reload = true;
	
	var tabDiv = $(divId);

	var liContainer = $("ul.nav-tabs");
	var divContainer = $("div.tab-content");
	
	// check if it's a dynamic or static tab
	if (url != undefined) { // dynamic tab
		if (tabDiv.length == 0) {
			// tab div not created yet, create one switch to it and load content
			tabDiv = $('<div class="tab-pane"><img src="'+assetsRoot+'images/loading.gif" /></div>')
						.prop("id", divId.replace(/^#/, ""))
						.appendTo(divContainer);
			reload = true;
		}
		
		activate(li, liContainer); // set active tab 
		activate(tabDiv, divContainer); // set active tab content

		if (reload == true) {
			// load its content
			$.get(url, function (data) {
				console.log("received data"); //console.debug(data);
				// replace loading anim with actual content
				tabDiv.html(data);
			});
		}
	}
	else { 	// static tab
		// tab div exists, switch to it
		activate(li, liContainer); // set active tab 
		activate(tabDiv, divContainer); // set active tab content
	}
}

/**
 * from bootstrap-tab.js
 */
function activate(element, container) {
	var $active = container.find('> .active');
	
    $active
    	.removeClass('active')
    	.find('> .dropdown-menu > .active')
    	.removeClass('active');

    element.addClass('active')

    if ( element.parent('.dropdown-menu') ) {
      element.closest('li.dropdown').addClass('active')
    }
}

/**
 * Install the given jquery button as the default button for input elements found
 * in given jquery container.
 * @param container
 * @param button
 */
function installDefaultButton(container, button) {
	$("input", container).keypress(function (e) {
		if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
            button.click();
            return false;
        } else {
        	return true;
        }
	});
}

/**
 * jumbotron sub nav scroll support (from twitter bootstrap docs (application.js))
 */
function installSubnavScrollSpy() {
    var $win = $(window)
    	, $nav = $('.subnav')
    	, navTop = $('.subnav').length && $('.subnav').offset().top - 40 // the value here is the height of subnav
    	, isFixed = 0;
    
    processScroll();

    $win.on("scroll", processScroll);
	
	function processScroll() {
		var i, scrollTop = $win.scrollTop();
		if (scrollTop >= navTop && !isFixed) {
			isFixed = 1;
			$nav.addClass('subnav-fixed');
		} else if (scrollTop <= navTop && isFixed) {
			isFixed = 0;
			$nav.removeClass('subnav-fixed');
		}
	}
}

/**
 * Post the simulated current date/time
 * @param dateTimeStr a date that matches the format "dd/MM/yyyy HH:mm"
 */
function setCurrentDateTime(dateTimeStr) {
	// expecting a date with format "dd/MM/yyyy HH:mm"
	console.log("setCurrentDateTime()");
	console.log("  posting dateTime " + dateTimeStr);
	$.post("/admin/currentDateTime",
			{dateTime: dateTimeStr},
			function(data) {
				console.log("Ok ! currentDateTime updated");
			}
	);
}

$(function() {
	// setup default ajax error handler function
	$.ajaxSetup({
		error: function (jqXHR) {
			console.log("Ajax error ! status = ["+jqXHR.status+"] response = ["+jqXHR.responseText+"]");
		}
	});
});

