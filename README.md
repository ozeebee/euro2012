
# About

Ok, this project is an attempt to create a web application that offers a forecasting system for Euro 2012 football competition.
My main motivation is to learn the Scala language and the Play 2.0 framework as well as HTML5 and other 'trendy' libs/frameworks (Bootstrap, LESS, ...)
I would be surprised if this app ends up in a production environment for real use, but who knows ...

##TODO:
	- ! Unit test for ranking system (as well as for group standings)
	- add responsive (bootstrap) variation to navbar
	- refactor main.less to use bootstrap mixins
	- add test for user registration (validation) and for authentication (check secured pages cannot be accessed anonymously)
	- add user role (for administration pages)
	- ? twitter integration
	- i18n
	- Play Bug with batch updates : the call to addBatch() must be done after settings parameters
	- Test MongoDB ! (get rid of the relational model)
	- CSS code cleanup (remove unused classes after refactoring)

## Home Pages

- Dashboard, to display :
	- upcoming matches (filter) (see calendar)
	- rankings (top 5/10) + current player position and points
	- calendar ?
	- connected players ? (with chat functionality)
	- twitter notifs ?
- show final competition (and forecast) winner once competition is finished
	
## Competition

- view teams/groups/schedules/standings
- ? calendar
!! - store winner name somewhere (since draws are possible results even in direct confrontation games)

## Game

- forecasts
	- add saveall button to forecasts page (at bottom)
- rankings
	- link to other user forecasts (ONLY for matches already played)
	- stats à la vimeo.com (ex: http://vimeo.com/29763331)
- banner for live matches (with live score) : ah the bottom of the page (fixed pos) [like less site header]

## Admin
- stats pages with number of users , number of match with results, number of forecasts and percentages
- add way to set team names for Quarter/Semi/Final stages once dependent matches results are known (check on date with warning)

### User management
- create account
- ? user picture / gravatar integration
	==> add tip in register page to create a gravatar
- ? chat function (with WebSocket impl) ?
	
### Other
- JS : remove absolute urls in external javascripts
- Pjax : evaluate integration of pjax for page transitions; see project https://github.com/pvillega/pjax-Forms
- Mobile version ? with jquery mobile
- integrate with google analytics
- Caching of commonly accessed resources (teams, etc) (including in json requests)
?- captcha ?