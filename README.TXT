= UberMap =

last updated July 15th 2014

This is a sample app to showcase many aspects of Android Location Service and Google Maps SDK.

UI & Android SDK
- Drawer with Fragments (one for each Map Project) and activities (for About and Settings)
- Settings Fragment and registerOnSharedPreferenceChangeListener (can probably be done with Otto)
- Google Maps fragments
- Markers, add move animate
- Save Camera Position in Prefs
- Async Reverse Geocoding when the marker is dropped (with Retrofit)
- On Camera change, rotate bearing, calculate distance
- Location API, get your location, register for update (in progress)



API
- OpenMapQuest => Reverse Geocoding, gives name of place from coordinates
- NextBusAPi (SF Muni) => vehicles positions and speed
- TODO DemographicGovApi

LIBS
- Retrofit + SimpleXML for NextBusAPI
- Retrofit + Gson for OpenMapQuest API







KNOWN ISSUES
- About and Prefs in side drawers should not be highlighted when the have been closed



NEXT IDEAS

- Otto (change in Settings to affect immediately different parts of the app). Setting should already
support this, doesn't it?
- Update / Save your position as you move. Show a trail of where you have been.

MAYBE

- add a canvas on top of the map to draw something complex or to better animate hundreds of objects?