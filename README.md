
# About

Ok, this project is an attempt to create a web application that offers a forecasting system for Euro 2012 football competition.
My main motivation is to learn the Scala language and the Play 2.0 framework as well as HTML5 and other 'trendy' libs/frameworks (Bootstrap, LESS, ...)
I would be surprised if this app ends up in a production environment for real use, but who knows ...

##TODO:
	- add responsive (bootstrap) variation to navbar
	- refactor main.less to use bootstrap mixins
	- add test for user registration (validation) and for authentication (check secured pages cannot be accessed anonymously)
	- add user role (for administration pages)
	- ? twitter integration
	- i18n
	- Play Bug with batch updates : the call to addBatch() must be done after settings parameters
	- Test MongoDB ! (get rid of the relational model)

## Pages

- ? add statistics to home page ? (change homepage to dashboard like concept)
	
## Competition

- view teams/groups/schedules/standings
- ? calendar

## Game

- forecasts
- rankings

## Admin

### User management
- create account
- ? user picture / gravatar integration
- ? chat function (with WebSocket impl) ?
	
### Other
- JS : remove absolute urls in external javascripts
