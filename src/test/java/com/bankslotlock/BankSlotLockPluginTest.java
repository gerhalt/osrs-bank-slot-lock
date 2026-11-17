package com.bankslotlock;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BankSlotLockPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankSlotLockPlugin.class);
		RuneLite.main(args);
	}
}
