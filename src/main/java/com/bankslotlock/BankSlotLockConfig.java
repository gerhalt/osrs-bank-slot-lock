package com.bankslotlock;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BankSlotLockConfig.GROUP)
public interface BankSlotLockConfig extends Config
{
	String GROUP = "bankslotlock";
	/** Per-account (RuneScape profile) key holding the comma-separated locked item IDs. */
	String LOCKED_ITEMS_KEY = "lockedItems";

	@ConfigSection(
		name = "Locked items",
		description = "How locked items behave in the bank.",
		position = 10
	)
	String lockedSection = "locked";

	@ConfigSection(
		name = "Lock icon",
		description = "The padlock drawn on locked slots.",
		position = 20
	)
	String iconSection = "icon";

	enum ShiftClickMode
	{
		PLACEHOLDER("Placeholder", "Placeholder"),
		ALL_BUT_ONE("Withdraw-All-but-1", "Withdraw-All-but-1"),
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
		keyName = "shiftToToggle",
		name = "Shift to lock",
		description = "Only show Lock slot and Unlock slot in the menu while Shift is held.",
		position = 1
	)
	default boolean shiftToToggle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat messages",
		description = "Show a chat message when an item is locked or unlocked, or an action is blocked.",
		position = 2
	)
	default boolean chatMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideRelease",
		name = "Hide Release",
		description = "Remove Release from the menu of locked placeholders.",
		position = 1,
		section = lockedSection
	)
	default boolean hideRelease()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blockWithdrawX",
		name = "Hide Withdraw-X",
		description = "Remove Withdraw-X from the menu of locked items when placeholders are off.",
		position = 2,
		section = lockedSection
	)
	default boolean blockWithdrawX()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lockedShiftClick",
		name = "Shift-click",
		description = "What Shift-click Withdraw-All does on locked items when placeholders are off.",
		position = 3,
		section = lockedSection
	)
	default ShiftClickMode lockedShiftClick()
	{
		return ShiftClickMode.PLACEHOLDER;
	}

	@ConfigItem(
		keyName = "showIcon",
		name = "Show icon",
		description = "Draw a padlock on locked slots.",
		position = 1,
		section = iconSection
	)
	default boolean showIcon()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "iconColor",
		name = "Color",
		description = "The color of the padlock.",
		position = 2,
		section = iconSection
	)
	default Color iconColor()
	{
		return new Color(255, 200, 0, 220);
	}

	@ConfigItem(
		keyName = "iconPosition",
		name = "Position",
		description = "Which corner of the slot the padlock is drawn in.",
		position = 3,
		section = iconSection
	)
	default IconPosition iconPosition()
	{
		return IconPosition.BOTTOM_LEFT;
	}

	@Range(min = 25, max = 200)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "iconScale",
		name = "Size",
		description = "The size of the padlock.",
		position = 4,
		section = iconSection
	)
	default int iconScale()
	{
		return 50;
	}
}
