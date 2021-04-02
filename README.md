# OBS Tally

## What is OBS Tally?
OBS tally is an android overlay application that allows cameramen to see if their camera is live on [OBS Studio](https://obsproject.com)

I am using [RTSP Camera Server](https://play.google.com/store/apps/details?id=com.miv.rtspcamera) as a RTSP media source for [OBS Studio](https://obsproject.com),
this works great, but on a multicam setup, the cameramen need to be aware if their camera are live, preview or clear to move.

This application handles that by adding a tally light equivalent as an overlay on the android device screen.

## Requirements
Please read [How-to Start](#How-to-Start) if you do not have those installed.

* [OBS Studio](https://obsproject.com)
* [RabbitMQ](https://www.rabbitmq.com) - mine run on [Docker](https://hub.docker.com/_/rabbitmq)
* curl installed on your system
* An android device

## Disclamer
This has been tested on MacOS, I do not know if it works with Windows.

Curl seems to be included in Windows 10, so hopefully it should work.

If not, you may try to change the command line called by the script.

I tried using a lua http library but OBS does not include it by default.

## How-to Start
* [Download and install OBS Studio](https://obsproject.com/wiki/install-instructions)
* [Create your Scenes and Sources as you like](https://obsproject.com/wiki/OBS-Studio-Overview#scenes-and-sources)
* [Install and run RabbitMQ](https://www.rabbitmq.com/download.html)

I like to run mine on Docker:
```bash
docker run -d --name obs-tally -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=<username> -e RABBITMQ_DEFAULT_PASS=<password> rabbitmq:3-management
```

Do not forget to replace <username> and <password> by the corresponding values in the command line

* Go to [http://localhost:15672/#/exchanges](http://localhost:15672/#/exchanges) and create a new exchange called `cam` with type `fanout`
* Download the script [OBS-Tally.lua](OBS-Tally.lua)
* Replace <username> and <password> by the corresponding values in the file
* Open OBS Studio and click on the menu `Tools` > `Scripts`
* Click on the `+` sign on the bottom left of the window and select the script `OBS_Tally.lua`
* Configure the script with `localhost` as endpoint and cam numbers on each scene (if there is no camera on a scene, use 0 as a placeholder)
* Before building the Android app, create a `amqp.properties` file based on [`amqp.properties.sample`](amqp.properties.sample) and replace <username> and <password> by the corresponding values in the file
* Install the app on your android device and start it
* At first run you will be prompted to accept to run the app as an overlay
* Then enter the IP of your computer as endpoint and the cam number and click on `start`
* Then switch between your scenes as usual:
    - The number displayed on the tally overlay is always the Program
    - If your camera is the Program => the tally overlay will have a red background
    - Else if your camera is in Preview => the tally overlay will have a green background
    - Else the tally overlay will have a gray background
