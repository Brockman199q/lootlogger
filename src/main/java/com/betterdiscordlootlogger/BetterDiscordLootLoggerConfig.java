package com.betterdiscordlootlogger;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup("discorduniquesnotifications")
public interface BetterDiscordLootLoggerConfig extends Config
{
    @ConfigSection(
            name = "What to Screenshot",
            description = "All the options that select what to screenshot",
            position = 99
    )
    String whatSection = "what";

//	@ConfigSection(
//			name = "Other Options",
//			description = "All the other options",
//			position = 98
//	)
//	String discordSection = "other";

    @ConfigItem(
            keyName = "sendScreenshot",
            name = "Send Screenshot",
            description = "Include a screenshot when levelling up.",
            position = 3
    )
    default boolean sendScreenshot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "hotkey",
            name = "Screenshot hotkey",
            description = "When you press this key a screenshot will be taken",
            position = 4
    )
    default Keybind hotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "rewards",
            name = "Screenshot Rewards",
            description = "Configures whether screenshots are taken of clues, barrows, and quest completion",
            position = 3,
            section = whatSection
    )
    default boolean screenshotRewards()
    {
        return true;
    }

    @ConfigItem(
            keyName = "levels",
            name = "Screenshot Levels",
            description = "Configures whether screenshots are taken of level ups",
            position = 4,
            section = whatSection
    )
    default boolean screenshotLevels()
    {
        return true;
    }

    @ConfigItem(
            keyName = "pets",
            name = "Screenshot Pet",
            description = "Configures whether screenshots are taken of receiving pets",
            position = 5,
            section = whatSection
    )
    default boolean screenshotPet()
    {
        return true;
    }

    @ConfigItem(
            keyName = "valuableDrop",
            name = "Screenshot Valuable drops",
            description = "Configures whether or not screenshots are automatically taken when you receive a valuable drop.",
            position = 6,
            section = whatSection
    )
    default boolean screenshotValuableDrop()
    {
        return false;
    }

    @ConfigItem(
            keyName = "valuableDropThreshold",
            name = "Valuable Threshold",
            description = "The minimum value to save screenshots of valuable drops.",
            position = 7,
            section = whatSection
    )
    default int valuableDropThreshold()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "collectionLogEntries",
            name = "Screenshot collection log entries",
            description = "Take a screenshot when completing an entry in the collection log",
            position = 8,
            section = whatSection
    )
    default boolean screenshotCollectionLogEntries()
    {
        return true;
    }

    @ConfigItem(
            keyName = "combatAchievements",
            name = "Screenshot combat achievements",
            description = "Take a screenshot when completing a combat achievement task",
            position = 8,
            section = whatSection
    )
    default boolean screenshotCombatAchievements()
    {
        return true;
    }

    @ConfigItem(
            keyName = "webhook",
            name = "Discord Webhook",
            description = "The webhook used to send messages to Discord."
    )
    String webhook();

//	@ConfigItem(
//			keyName = "includeUsername",
//			name = "Include username",
//			description = "Configures if the discord embed will include your username",
//			position = 12,
//			section = discordSection
//	)
//	default boolean includeUsername()
//	{
//		return true;
//	}
}
