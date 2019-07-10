# SoundCloud Telegeram Bot
Simple bot for [Telegram Messenger](https://telegram.org/). 
Able to search and download tracks from SoundCloud using link or text.

Bot is available at [@scloaderbot](https://t.me/scloaderbot).

## Usage
1. Change ```config.properties``` to run the application  
with appropriate credentials.
2. Build project with Maven ```./mwnw clean package```
3. Run:
    ##### Plain run
- ```java -jar <path_to_jar>```
    ##### Or run inside Docker
- ```docker build -t soundcloudbot .```
- ```docker run soundcloudbot```
## TODO
- 'Send audio' indicator while sending track.
