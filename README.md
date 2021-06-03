# Guifena-Transmitter
This app is function as a transmitter that capture loud audio and then transmit the audio to the cloud for further processing.


# Architecture
This app writte using kotlin.

For the network this app using Post and Get Request method using retrofit2.

This app use android Foreground Service to do recording in periodic time.


# Features
The main function of this app is to record periodicly with certai interval using foreground service and then send the data to the webservice, server, etc.


# Configure the BASE URL of the server and API Interface
You may configure the project to using your own server. You just need to change the BASE URL at RetrofitBuilder class and adjust the API interface.


# How to use
In order to start the service first you need to set the name and location of the device.

<a href="https://ibb.co/41ZK0qq"><img src="https://i.ibb.co/WVWGRhh/Whats-App-Image-2021-06-03-at-20-51-01.jpg" height="580" width="280" ></a>
<a href="https://ibb.co/7SSsr2Z"><img src="https://i.ibb.co/2FFGcgC/Whats-App-Image-2021-06-03-at-20-51-05.jpg" height="580" width="280" ></a>
<a href="https://ibb.co/LSXVXdK"><img src="https://i.ibb.co/gyLkLZY/Whats-App-Image-2021-06-03-at-20-50-46.jpg" height="580" width="280" ></a>


<br>

You can optionaly configure the interval of the recording and the threshold of how much dB that the audio need to meet so the audio will be send to the server.

You can set the threshold as a decibel and record interval as seconds.


<a href="https://ibb.co/3r0ZZDJ"><img src="https://i.ibb.co/FxBTTSP/Whats-App-Image-2021-06-03-at-21-07-44.jpg" height="580" width="280"></a>

