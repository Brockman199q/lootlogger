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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup("betterdiscordlootlogger")
public interface BetterDiscordLootLoggerConfig extends Config
{
    @ConfigSection(
            name = "Choose what to send",
            description = "Choose from the options below which events you would like to send",
            position = 99
    )
    String whatToSendSection = "what to send";

    @ConfigItem(
            keyName = "sendScreenshot",
            name = "Send Screenshot?",
            description = "Include a screenshot in the discord message?",
            position = 3
    )
    default boolean sendScreenshot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "keybind",
            name = "Screenshot Keybind",
            description = "Add keybind to manually take a screenshot and send a message of your rare drop",
            position = 4
    )
    default Keybind keybind()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "pets",
            name = "Include Pets",
            description = "Configures whether new pets will be automatically sent to discord",
            position = 5,
            section = whatToSendSection
    )
    default boolean includePets()
    {
        return true;
    }

    @ConfigItem(
            keyName = "valuableDrop",
            name = "Include Valuable drops",
            description = "Configures whether valuable drops will be automatically sent to discord.",
            position = 6,
            section = whatToSendSection
    )
    default boolean includeValuableDrops()
    {
        return false;
    }

    @ConfigItem(
            keyName = "valuableDropThreshold",
            name = "Valuable Drop Threshold",
            description = "The minimum value of drop for it to send a discord message.",
            position = 7,
            section = whatToSendSection
    )
    default int valuableDropThreshold()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "collectionLogItem",
            name = "Include collection log items",
            description = "Configures whether a message will be automatically sent to discord when you obtain a new collection log item.",
            position = 8,
            section = whatToSendSection
    )
    default boolean includeCollectionLogItems()
    {
        return true;
    }

	@ConfigItem(
		keyName = "raidLoot",
		name = "Include raid loot (Experimental)",
		description = "Configures whether a message will be automatically sent to discord when you obtain a raid unique.",
		position = 8,
		section = whatToSendSection
	)
	default boolean includeRaidLoot()
	{
		return true;
	}

    @ConfigItem(
            keyName = "webhook",
            name = "Discord Webhook",
            description = "The webhook used to send messages to Discord."
    )
    String webhook();
}
