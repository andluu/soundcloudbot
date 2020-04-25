# SoundCloud Telegeram Bot
Simple bot for [Telegram Messenger](https://telegram.org/). 
Able to search and download tracks from SoundCloud using link or text.

Bot is available at [@soundcloudloader_bot](https://t.me/soundcloudloader_bot).

## Usage
1. Add these environment variables
 - ```BOT_TOKEN``` : your Telegram Bot token
 - ```BOT_USERNAME``` : your Telegram Bot username
 - ```CLIENT_ID``` : SoundCloud token client ID
2. Build project with Maven ```./mwnw clean package```
3. Run:
    ##### Plain run
- ```java -jar <path_to_jar>```
    ##### Or run inside Docker
- ```docker build -t soundcloudbot .```
- ```docker run soundcloudbot```
## TODO
- <del> 'Send audio' indicator while sending track. </del>
