
# About

Ok, this project is an attempt to create a web application that offers a forecasting system for Euro 2012 football competition.
My main motivation is to learn the Scala language and the Play 2.0 framework as well as HTML5 and other 'trendy' libs/frameworks (Bootstrap, LESS, ...)
I would be surprised if this app ends up in a production environment for real use, but who knows ...

##TODO:
	- ! Unit test for ranking system (as well as for group standings)
	- add responsive (bootstrap) variation to navbar
	- refactor main.less to use bootstrap mixins
	- ? twitter integration
	- i18n
	- Test MongoDB ! (get rid of the relational model)
	- CSS code cleanup (remove unused classes after refactoring)

## Features

### Home Pages

- colors:
	body bg color and portlet colors like akka doc and code blocks + portlet shadow boxes
- Dashboard, to display :
	- upcoming matches (filter) (see calendar)
	- rankings (top 5/10) + current player position and points
	- calendar ?
	- connected players ? (with chat functionality)
	- twitter notifs ?
- show final competition (and forecast) winner once competition is finished
	
### Competition

- add Next/Previous buttons on Group pages
- team details (useful ?)
- ? calendar
!! - store winner name somewhere (since draws are possible results even in direct confrontation games)

### Game

- forecasts
	- add saveall button to forecasts page (at bottom)
- rankings
	- revampt with amen-style score chart (getamen.com)
	- link to other user forecasts (ONLY for matches already played)
	- stats à la vimeo.com (ex: http://vimeo.com/29763331)
- banner for live matches (with live score) : ah the bottom of the page (fixed pos) [like less site header]

### Admin
- stats pages with number of users , number of match with results, number of forecasts and percentages
- add way to set team names for Quarter/Semi/Final stages once dependent matches results are known (check on date with warning)

### User management
- refactor account creation workflow to include 2 steps user creation : with creation request and validation
	- create account page with ability to delete user, change pwd
- ? chat function (with WebSocket impl) ?
- add account page
	
### Other
- add license page with all applicable license infos (icons, libs, etc)
- JS : remove absolute urls in external javascripts ===> use JavaScript router instead of passing urls from templates
- Pjax : evaluate integration of pjax for page transitions; see project https://github.com/pvillega/pjax-Forms
- Mobile version ? with jquery mobile
- integrate with google analytics
- Caching of commonly accessed resources (teams, etc) (including in json requests)
?- captcha ?
