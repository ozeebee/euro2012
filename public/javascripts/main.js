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

	var liContainer = $("ul.tabs");
	var divContainer = $("div.tab-content");
	
	// check if the div is already (loaded) into the dom
	if (tabDiv.length == 0) {
		// tab div not created yet, create one switch to it and load content
		tabDiv = $('<div class="tab-pane"><img src="'+assetsRoot+'images/loading.gif" /></div>')
					.prop("id", divId.replace(/^#/, ""))
					.appendTo(divContainer);
		
		activate(li, liContainer); // set active tab 
		activate(tabDiv, divContainer); // set active tab content

		// load its content
		$.get(url, function (data) {
			console.log("received data"); //console.debug(data);
			// replace loading anim with actual content
			tabDiv.html(data);
		});
	}
	else {
		// tab div exists, switch to it
		activate(li, liContainer); // set active tab 
		activate(tabDiv, divContainer); // set active tab content
	}
}

/**
 * from bootstrap-tabs.js
 */
function activate(element, container) {
    container
      .find('> .active')
      .removeClass('active')
      .find('> .dropdown-menu > .active')
      .removeClass('active')

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
