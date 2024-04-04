Feature:  Hourly Forecast

Background:
  * url 'https://weather.cit.api.here.com/weather/1.0/report.json?product=forecast_hourly&name=Chicago&app_id={{YOUR_APP_ID}}&app_code={{YOUR_APP_CODE}}'

Scenario:  Hourly Forecast
  Given method GET
  When path '/'
  Then status 200

