missed-calls
============

test app for android that should add soundalert/reminder/notification for missed calls

generally the app consists of two activities and one service. the first activity has UI to start/stop the service,
the second is used to display a list of missed calls. the service runs in the background watching for missed calls,
there is some problem that sometimes it goes into deeper sleep and fails to create sound notifications, 
still wondering what the problem might be.

pretty much an app to get me familiar with the android platform; i last wrote java about 10 years ago so sorry for messy code

tested on htc magic with stock android 1.5

