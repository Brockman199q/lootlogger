/*
 * Copyright (c) 2022, RinZ
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.betterdiscordlootlogger;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
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
import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(
	name = "Better Discord Loot Logger"
)
public class BetterDiscordLootLoggerPlugin extends Plugin
{
	private static final String COLLECTION_LOG_TEXT = "New item added to your collection log: ";
	private static final Pattern VALUABLE_DROP_PATTERN = Pattern.compile(".*Valuable drop: ([^<>]+?\\(((?:\\d+,?)+) coins\\))(?:</col>)?");
	private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of("You have a funny feeling like you're being followed",
		"You feel something weird sneaking into your backpack",
		"You have a funny feeling like you would have been followed");

	private boolean shouldSendMessage;

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

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.keybind())
	{
		@Override
		public void hotkeyPressed()
		{
			sendMessage("", "", "manual");
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
		} else {
			shouldSendMessage = true;
		}
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

		if (config.includePets() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			sendMessage("", "", "pet");
		}

		if (config.includeValuableDrops())
		{
			Matcher m = VALUABLE_DROP_PATTERN.matcher(chatMessage);
			if (m.matches())
			{
				int valuableDropValue = Integer.parseInt(m.group(2).replaceAll(",", ""));
				if (valuableDropValue >= config.valuableDropThreshold())
				{
					String[] valuableDrop = m.group(1).split(" \\(");
					String valuableDropName = (String) Array.get(valuableDrop, 0);
					String valuableDropValueString = m.group(2);
					sendMessage(valuableDropName, valuableDropValueString, "valuable drop");
				}
			}
		}

		if (config.includeCollectionLogItems() && chatMessage.startsWith(COLLECTION_LOG_TEXT) && (client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 1 || client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) == 3))
		{
			String entry = Text.removeTags(chatMessage).substring(COLLECTION_LOG_TEXT.length());
			sendMessage(entry, "", "collection log");
		}
	}

	@Provides
	BetterDiscordLootLoggerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterDiscordLootLoggerConfig.class);
	}

	private void sendMessage(String itemName, String itemValue, String notificationType)
	{
		if (!shouldSendMessage) {return;}

		switch (notificationType)
		{
			case "pet":
				itemName = "a new pet";
				break;
			case "chest":
				itemName = "**" + itemName + "**";
				break;
			case "valuable drop":
				itemName = " a valuable drop: **" + itemName + "**";
				break;
			case "collection log":
				itemName = " a new collection log item: **" + itemName + "**";
				break;
			case "combat task":
				itemName = " a new combat task achievement: **" + itemName + "**";
				break;
			default:
				itemName = " **a rare drop**";
				break;
		}

		String screenshotString = "**" + client.getLocalPlayer().getName() + "**";
		if (!itemValue.isEmpty())
		{
			screenshotString += " just received" + itemName + "! \nApprox Value: **" + itemValue + " coins**";
		}
		else
		{
			screenshotString += " just received" + itemName + "!";
		}


		com.betterdiscordlootlogger.DiscordWebhookBody discordWebhookBody = new com.betterdiscordlootlogger.DiscordWebhookBody();
		discordWebhookBody.setContent(screenshotString);
		sendWebhook(discordWebhookBody);
	}

	private void sendWebhook(com.betterdiscordlootlogger.DiscordWebhookBody discordWebhookBody)
	{
		String configUrl = config.webhook();
		if (Strings.isNullOrEmpty(configUrl))
		{
			return;
		}

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
	}
}
