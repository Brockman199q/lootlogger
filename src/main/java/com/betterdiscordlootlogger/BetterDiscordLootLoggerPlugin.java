package com.betterdiscordlootlogger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.UsernameChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
        name = "Discord Unique Notifications"
)
public class BetterDiscordLootLoggerPlugin extends Plugin
{
    private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";
    private static final String CHEST_LOOTED_MESSAGE = "You find some treasure in the chest!";
    private static final Map<Integer, String> CHEST_LOOT_EVENTS = ImmutableMap.of(12127, "The Gauntlet");
    private static final int GAUNTLET_REGION = 7512;
    private static final int CORRUPTED_GAUNTLET_REGION = 7768;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9]+)");
    private static final Pattern VALUABLE_DROP_PATTERN = Pattern.compile(".*Valuable drop: ([^<>]+?\\(((?:\\d+,?)+) coins\\))(?:</col>)?");
    //	private static final Pattern COMBAT_ACHIEVEMENTS_PATTERN = Pattern.compile("Congratulations, you've completed an? (?<tier>\\w+) combat task: <col=[0-9a-f]+>(?<task>(.+))</col>\\.");
    private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of("You have a funny feeling like you're being followed",
            "You feel something weird sneaking into your backpack",
            "You have a funny feeling like you would have been followed");
    private static final String SD_CLUE_SCROLL_REWARDS = "Clue Scroll Rewards";
    private static final String SD_PETS = "Pets";
    private static final String SD_CHEST_LOOT = "Chest Loot";
    private static final String SD_VALUABLE_DROPS = "Valuable Drops";
    private static final String SD_COLLECTION_LOG = "Collection Log";
    private static final String SD_COMBAT_ACHIEVEMENTS = "Combat Achievements";

    private String clueType;
    private Integer clueNumber;

    enum KillType
    {
        BARROWS,
        COX,
        COX_CM,
        TOB,
        TOB_SM,
        TOB_HM
    }

    private KillType killType;
    private Integer killCountNumber;
    private boolean shouldSendMessage = false;
    private int ticksWaited = 0;

    @Inject
    private Client client;

    @Inject
    private BetterDiscordLootLoggerConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private KeyManager keyManager;

    @Inject
    private DrawManager drawManager;

    private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            shouldSendMessage = true;
        }
    };

    @Override
    protected void startUp() throws Exception
    {
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    protected void shutDown() throws Exception
    {
        keyManager.unregisterKeyListener(hotkeyListener);
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged)
    {
        resetState();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN))
        {
            resetState();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!shouldSendMessage)
        {
            return;
        }

        if (ticksWaited < 2)
        {
            ticksWaited++;
            return;
        }

        shouldSendMessage = false;
        ticksWaited = 0;
        sendMessage();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.TRADE
                && event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION)
        {
            return;
        }

        String chatMessage = event.getMessage();

        if (chatMessage.contains("You have completed") && chatMessage.contains("Treasure"))
        {
            Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
            if (m.find())
            {
                clueNumber = Integer.valueOf(m.group());
                clueType = chatMessage.substring(chatMessage.lastIndexOf(m.group()) + m.group().length() + 1, chatMessage.indexOf("Treasure") - 1);
                return;
            }
        }

        if (chatMessage.startsWith("Your Barrows chest count is"))
        {
            Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
            if (m.find())
            {
                killType = KillType.BARROWS;
                killCountNumber = Integer.valueOf(m.group());
                return;
            }
        }

        if (chatMessage.startsWith("Your completed Chambers of Xeric count is:"))
        {
            Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
            if (m.find())
            {
                killType = KillType.COX;
                killCountNumber = Integer.valueOf(m.group());
                return;
            }
        }

        if (chatMessage.startsWith("Your completed Chambers of Xeric Challenge Mode count is:"))
        {
            Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
            if (m.find())
            {
                killType = KillType.COX_CM;
                killCountNumber = Integer.valueOf(m.group());
                return;
            }
        }

        if (chatMessage.startsWith("Your completed Theatre of Blood"))
        {
            Matcher m = NUMBER_PATTERN.matcher(Text.removeTags(chatMessage));
            if (m.find())
            {
                killType = chatMessage.contains("Hard Mode") ? KillType.TOB_HM : (chatMessage.contains("Story Mode") ? KillType.TOB_SM : KillType.TOB);
                killCountNumber = Integer.valueOf(m.group());
                return;
            }
        }

        if (config.screenshotPet() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
        {
            shouldSendMessage = true;
        }

        if (chatMessage.equals(CHEST_LOOTED_MESSAGE) && config.screenshotRewards())
        {
            final int regionID = client.getLocalPlayer().getWorldLocation().getRegionID();
            String eventName = CHEST_LOOT_EVENTS.get(regionID);
            if (eventName != null)
            {
                shouldSendMessage = true;
            }
        }

        if (config.screenshotValuableDrop())
        {
            Matcher m = VALUABLE_DROP_PATTERN.matcher(chatMessage);
            if (m.matches())
            {
                int valuableDropValue = Integer.parseInt(m.group(2).replaceAll(",", ""));
                if (valuableDropValue >= config.valuableDropThreshold())
                {
//					String valuableDropName = m.group(1);
//					String fileName = "Valuable drop " + valuableDropName;
                    shouldSendMessage = true;
                }
            }
        }

        if (config.screenshotCollectionLogEntries() && chatMessage.startsWith(COLLECTION_LOG_TEXT) && (client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 1 || client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 3))
        {
//			String entry = Text.removeTags(chatMessage).substring(COLLECTION_LOG_TEXT.length());
//			String fileName = "Collection log (" + entry + ")";
            shouldSendMessage = true;
        }

        if (chatMessage.contains("combat task") && config.screenshotCombatAchievements() && client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 1)
        {
//			String fileName = parseCombatAchievementWidget(chatMessage);
//			if (!fileName.isEmpty())
//			{
            shouldSendMessage = true;
//			}
        }
    }

//	@Subscribe
//	public void onWidgetLoaded(WidgetLoaded event)
//	{
//		int groupId = event.getGroupId();
//
//		switch (groupId)
//		{
//			case QUEST_COMPLETED_GROUP_ID:
//			case CLUE_SCROLL_REWARD_GROUP_ID:
//			case CHAMBERS_OF_XERIC_REWARD_GROUP_ID:
//			case THEATRE_OF_BLOOD_REWARD_GROUP_ID:
//			case BARROWS_REWARD_GROUP_ID:
//				if (!config.screenshotRewards())
//				{
//					return;
//				}
//				break;
//		}
//
//		switch (groupId)
//		{
//			case CHAMBERS_OF_XERIC_REWARD_GROUP_ID:
//			{
//				if (killType == KillType.COX)
//				{
//					killType = null;
//					killCountNumber = 0;
//					break;
//				}
//				else if (killType == KillType.COX_CM)
//				{
//					killType = null;
//					killCountNumber = 0;
//					break;
//				}
//				return;
//			}
//			case THEATRE_OF_BLOOD_REWARD_GROUP_ID:
//			{
//				if (killType != KillType.TOB && killType != KillType.TOB_SM && killType != KillType.TOB_HM)
//				{
//					return;
//				}
//
//				switch (killType)
//				{
//					case TOB:
//						break;
//					case TOB_SM:
//						break;
//					case TOB_HM:
//						break;
//					default:
//						throw new IllegalStateException();
//				}
//				killType = null;
//				killCountNumber = 0;
//				break;
//			}
//			case BARROWS_REWARD_GROUP_ID:
//			{
//				if (killType != KillType.BARROWS)
//				{
//					return;
//				}
//				killType = null;
//				killCountNumber = 0;
//				break;
//			}
//			case CLUE_SCROLL_REWARD_GROUP_ID:
//			{
//				if (clueType == null || clueNumber == null)
//				{
//					return;
//				}
//
//				clueType = null;
//				clueNumber = null;
//				break;
//			}
//			default:
//				return;
//		}
//
//		shouldSendMessage = true;
//	}
//	@Subscribe
//	public void onStatChanged(net.runelite.api.events.StatChanged statChanged)
//	{
//		String skillName = statChanged.getSkill().getName();
//		int level = statChanged.getLevel();
//
//		// .contains wasn't behaving so I went with == null
//		if (currentLevels.get(skillName) == null || currentLevels.get(skillName) == 0)
//		{
//			currentLevels.put(skillName, level);
//			return;
//		}
//
//		if (currentLevels.get(skillName) != level)
//		{
//			currentLevels.put(skillName, level);
//
//			if (level >= config.minLevel())
//			{
//				leveledSkills.add(skillName);
//				shouldSendMessage = true;
//			}
//		}
//	}

    @Provides
    BetterDiscordLootLoggerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BetterDiscordLootLoggerConfig.class);
    }

    private void sendMessage()
    {
        String screenshotString = client.getLocalPlayer().getName() + " just received a rare drop! Say gz or else :)";
        screenshotString += " just recieved a rare drop! Say gz or else :)";

        com.betterdiscordlootlogger.DiscordWebhookBody discordWebhookBody = new com.betterdiscordlootlogger.DiscordWebhookBody();
        discordWebhookBody.setContent(screenshotString);
        sendWebhook(discordWebhookBody);
    }

    private void sendWebhook(com.betterdiscordlootlogger.DiscordWebhookBody discordWebhookBody)
    {
        String configUrl = config.webhook();
        if (Strings.isNullOrEmpty(configUrl)) { return; }

        HttpUrl url = HttpUrl.parse(configUrl);
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));

        if (config.sendScreenshot())
        {
            sendWebhookWithScreenshot(url, requestBodyBuilder);
        }
        else
        {
            buildRequestAndSend(url, requestBodyBuilder);
        }
    }

    private void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
    {
        drawManager.requestNextFrameListener(image ->
        {
            BufferedImage bufferedImage = (BufferedImage) image;
            byte[] imageBytes;
            try
            {
                imageBytes = convertImageToByteArray(bufferedImage);
            }
            catch (IOException e)
            {
                log.warn("Error converting image to byte array", e);
                return;
            }

            requestBodyBuilder.addFormDataPart("file", "image.png",
                    RequestBody.create(MediaType.parse("image/png"), imageBytes));
            buildRequestAndSend(url, requestBodyBuilder);
        });
    }

    private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder)
    {
        RequestBody requestBody = requestBodyBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        sendRequest(request);
    }

    private void sendRequest(Request request)
    {
        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Error submitting webhook", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                response.close();
            }
        });
    }

    private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void resetState()
    {
        shouldSendMessage = false;
        ticksWaited = 0;
    }

    private boolean isInsideGauntlet()
    {
        return this.client.isInInstancedRegion()
                && this.client.getMapRegions().length > 0
                && (this.client.getMapRegions()[0] == GAUNTLET_REGION
                || this.client.getMapRegions()[0] == CORRUPTED_GAUNTLET_REGION);
    }

    @VisibleForTesting
    int getClueNumber()
    {
        return clueNumber;
    }

    @VisibleForTesting
    String getClueType()
    {
        return clueType;
    }

    @VisibleForTesting
    KillType getKillType()
    {
        return killType;
    }

    @VisibleForTesting
    int getKillCountNumber()
    {
        return killCountNumber;
    }
}
