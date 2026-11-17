package com.bankslotlock;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BankSlotLockConfig.GROUP)
public interface BankSlotLockConfig extends Config
{
	String GROUP = "bankslotlock";
	/** Per-account (RuneScape profile) key holding the comma-separated locked item IDs. */
	String LOCKED_ITEMS_KEY = "lockedItems";

	@ConfigItem(
		keyName = "shiftToToggle",
		name = "Require Shift",
		description = "Only show Lock slot when Shift is held",
		position = 1
	)
	default boolean shiftToToggle()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideRelease",
		name = "Hide Release",
		description = "Hide Release on locked placeholders",
		position = 2
	)
	default boolean hideRelease()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blockWithdrawX",
		name = "Hide Withdraw-X",
		description = "Hide Withdraw-X on locked items when placeholders are off",
		position = 3
	)
	default boolean blockWithdrawX()
	{
		return true;
	}

	enum ShiftClickMode
	{
		PLACEHOLDER("Placeholder", "Placeholder"),
		ALL_BUT_ONE("All-but-1", "Withdraw-All-but-1"),
		OFF("Off", null);

		private final String label;
		/** The bank menu option to swap to, or null for no swap. */
		private final String option;

		ShiftClickMode(String label, String option)
		{
			this.label = label;
			this.option = option;
		}

		String getOption()
		{
			return option;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "lockedShiftClick",
		name = "Locked shift-click",
		description = "What Shift-click Withdraw-All does on locked items when placeholders are off",
		position = 4
	)
	default ShiftClickMode lockedShiftClick()
	{
		return ShiftClickMode.PLACEHOLDER;
	}

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat messages",
		description = "Show a message when an action is blocked",
		position = 5
	)
	default boolean chatMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showIcon",
		name = "Show lock icon",
		description = "Show a padlock on locked slots",
		position = 6
	)
	default boolean showIcon()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "iconColor",
		name = "Icon colour",
		description = "Colour of the padlock",
		position = 7
	)
	default Color iconColor()
	{
		return new Color(255, 200, 0, 220);
	}

	enum IconPosition
	{
		// No top left: the stack size is drawn there.
		TOP_RIGHT("Top right"),
		BOTTOM_LEFT("Bottom left"),
		BOTTOM_RIGHT("Bottom right");

		private final String label;

		IconPosition(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "iconPosition",
		name = "Icon position",
		description = "Corner of the slot to show the padlock in",
		position = 8
	)
	default IconPosition iconPosition()
	{
		return IconPosition.BOTTOM_LEFT;
	}

	@Range(min = 25, max = 200)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "iconScale",
		name = "Icon size",
		description = "Size of the padlock",
		position = 9
	)
	default int iconScale()
	{
		return 50;
	}
}
