# Better Discord Loot Logger
This plugin will take a screenshot (optionally) and send that along with a message via a discord webhook anytime you get a pet, valuable drop, raid item or collection log slot.

Main reason for making this over using any other one out there is that none of the others seemed to have support for collection log items.

### Setup
To set this up find the discord channel you want the notifications to go to and click the settings icon -> integrations -> webhooks -> New Webhook
Then copy and paste this webhook into the input field in the plugin and that's it!

### Changelog
- **08/07/23 (1.2)**
  -**Improvement** - Now allows multiple webhooks, seperated by commas. Thanks @Waldxn
- **4/6/22 (1.1)**  
  - **Improvement** - Screenshots of collection log entries will now be taken when the popup appears if it is enabled.
  - **New** - Added raid loot option that will include the item name, raid type (tob/cox/hard mode etc) and kill count in the message.
