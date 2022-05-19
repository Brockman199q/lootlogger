package com.betterdiscordlootlogger;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterDiscordLootLoggerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterDiscordLootLoggerPlugin.class);
		RuneLite.main(args);
	}
}