package com.bankslotlock;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Bank Slot Lock",
	description = "Lock bank items so their placeholder can't be released and the slot can't be emptied by a withdraw",
	tags = {"bank", "placeholder", "lock", "release", "withdraw", "protect"}
)
public class BankSlotLockPlugin extends Plugin
{
	private static final String LOCK = "Lock slot";
	private static final String UNLOCK = "Unlock slot";
	/** Bank item op 1 is the left-click withdraw at the current quantity setting. */
	private static final int LEFT_CLICK_OP = 1;

	@Inject
	private Client client;

	@Inject
	private BankSlotLockConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BankSlotLockOverlay overlay;

	/** Canonical (unnoted, non-placeholder) item IDs locked on the current account. */
	private Set<Integer> locked = Collections.emptySet();

	@Override
	protected void startUp()
	{
		loadLocks();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		locked = Collections.emptySet();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadLocks();
	}

	boolean isLocked(int itemId)
	{
		return !locked.isEmpty() && locked.contains(itemManager.canonicalize(itemId));
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		hideWithdrawAll(event.getMenuEntries());

		if (config.shiftToToggle() && !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		for (MenuEntry entry : event.getMenuEntries())
		{
			Widget widget = bankItemWidget(entry);
			if (widget == null)
			{
				continue;
			}

			int canonical = itemManager.canonicalize(widget.getItemId());
			boolean isLocked = locked.contains(canonical);
			String name = itemManager.getItemComposition(canonical).getName();

			// MenuAction.RUNELITE entries are handled client-side only and send nothing to the server.
			client.getMenu().createMenuEntry(1)
				.setOption(isLocked ? UNLOCK : LOCK)
				.setTarget(ColorUtil.wrapWithColorTag(name, JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> toggleLock(canonical, name));
			return;
		}
	}

	/**
	 * With placeholders off, turn a Shift-click Withdraw-All on a locked item into Placeholder
	 * (withdraw all, keep a placeholder) or Withdraw-All-but-1, depending on the setting.
	 * Runs after Menu Entry Swapper (lower priority) so it also catches MES's own shift-click swap.
	 */
	@Subscribe(priority = -1)
	public void onPostMenuSort(PostMenuSort event)
	{
		String swapTo = config.lockedShiftClick().getOption();
		if (swapTo == null
			|| client.isMenuOpen()
			|| !client.isKeyPressed(KeyCode.KC_SHIFT)
			|| client.getVarbitValue(VarbitID.BANK_LEAVEPLACEHOLDERS) == 1)
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}

		// The last entry is the left-click action.
		MenuEntry top = entries[entries.length - 1];
		Widget widget = bankItemWidget(top);
		if (widget == null
			|| !"Withdraw-All".equalsIgnoreCase(Text.removeTags(top.getOption()))
			|| !isLocked(widget.getItemId()))
		{
			return;
		}

		for (int i = entries.length - 2; i >= 0; i--)
		{
			MenuEntry entry = entries[i];
			if (entry.getParam1() == top.getParam1()
				&& entry.getParam0() == top.getParam0()
				&& swapTo.equalsIgnoreCase(Text.removeTags(entry.getOption())))
			{
				// Raise its priority so the client doesn't re-sort it below other ops.
				entry.setType(MenuAction.CC_OP);
				entries[i] = top;
				entries[entries.length - 1] = entry;
				menu.setMenuEntries(entries);
				return;
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry entry = event.getMenuEntry();
		String option = Text.removeTags(entry.getOption());
		boolean isRelease = "Release".equals(option);
		boolean isWithdraw = option.startsWith("Withdraw-");
		if (!isRelease && !isWithdraw)
		{
			return;
		}

		Widget widget = bankItemWidget(entry);
		if (widget == null || !isLocked(widget.getItemId()))
		{
			return;
		}

		if (isRelease)
		{
			if (config.hideRelease())
			{
				client.getMenu().removeMenuEntry(entry);
			}
			return;
		}

		if (shouldHideWithdraw(entry, option, widget.getItemQuantity()))
		{
			client.getMenu().removeMenuEntry(entry);
		}
	}

	/**
	 * With placeholders off, remove Withdraw-All from a locked item's right-click menu.
	 * Done when the menu opens rather than in onMenuEntryAdded, so the Shift-click
	 * All-but-1 swap can still find Withdraw-All while the left-click is being worked out.
	 */
	private void hideWithdrawAll(MenuEntry[] entries)
	{
		if (locked.isEmpty() || client.getVarbitValue(VarbitID.BANK_LEAVEPLACEHOLDERS) == 1)
		{
			return;
		}

		for (MenuEntry entry : entries)
		{
			if (entry.getIdentifier() == LEFT_CLICK_OP
				|| WithdrawAmount.parse(Text.removeTags(entry.getOption())) != WithdrawAmount.ALL)
			{
				continue;
			}

			Widget widget = bankItemWidget(entry);
			if (widget != null && isLocked(widget.getItemId()))
			{
				client.getMenu().removeMenuEntry(entry);
			}
		}
	}

	/**
	 * With placeholders off, hide Withdraw-X (if Hide Withdraw-X is on) and the remembered
	 * Withdraw-N entry when N would take the whole stack. Withdraw-All is hidden separately in
	 * onMenuOpened. Withdraw-1/5/10 and the left-click entry stay visible; clicking them is still
	 * blocked in onMenuOptionClicked.
	 */
	private boolean shouldHideWithdraw(MenuEntry entry, String option, int quantity)
	{
		if (client.getVarbitValue(VarbitID.BANK_LEAVEPLACEHOLDERS) == 1
			|| entry.getIdentifier() == LEFT_CLICK_OP)
		{
			return false;
		}

		int amount = WithdrawAmount.parse(option);
		if (amount == WithdrawAmount.UNKNOWN)
		{
			return config.blockWithdrawX();
		}
		return WithdrawAmount.isRememberedX(amount) && amount >= quantity;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (locked.isEmpty() || event.getMenuAction() == MenuAction.RUNELITE)
		{
			return;
		}

		if (event.getParam1() == InterfaceID.Bankmain.RELEASE_PLACEHOLDERS)
		{
			String lockedPlaceholder = firstLockedPlaceholder();
			if (lockedPlaceholder != null)
			{
				event.consume();
				notifyBlocked("Release all placeholders blocked: " + lockedPlaceholder + " is locked.");
			}
			return;
		}

		Widget widget = bankItemWidget(event.getMenuEntry());
		if (widget == null || !isLocked(widget.getItemId()))
		{
			return;
		}

		String option = Text.removeTags(event.getMenuOption());
		String name = itemManager.getItemComposition(itemManager.canonicalize(widget.getItemId())).getName();

		if ("Release".equals(option))
		{
			event.consume();
			notifyBlocked(name + " is locked, so its placeholder can't be released.");
			return;
		}

		boolean placeholdersOn = client.getVarbitValue(VarbitID.BANK_LEAVEPLACEHOLDERS) == 1;
		if (placeholdersOn)
		{
			return;
		}

		int amount = WithdrawAmount.parse(option);
		if (WithdrawAmount.empties(amount, widget.getItemQuantity(), config.blockWithdrawX()))
		{
			event.consume();
			notifyBlocked(name + " is locked and placeholders are off, so " + option + " would empty its slot.");
		}
	}

	/** Returns the bank grid widget an entry belongs to, or null if it isn't a real bank item. */
	private Widget bankItemWidget(MenuEntry entry)
	{
		if (entry == null || entry.getParam1() != InterfaceID.Bankmain.ITEMS)
		{
			return null;
		}

		Widget widget = entry.getWidget();
		if (widget == null || widget.getItemId() <= 0)
		{
			return null;
		}
		return widget;
	}

	/** Name of a locked item currently sitting in the bank as a placeholder, or null. */
	private String firstLockedPlaceholder()
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null)
		{
			// Can't tell what's in the bank, so err on the side of keeping the placeholders.
			return "an item";
		}

		for (Item item : bank.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}

			ItemComposition comp = itemManager.getItemComposition(item.getId());
			if (comp.getPlaceholderTemplateId() != -1 && locked.contains(comp.getPlaceholderId()))
			{
				return itemManager.getItemComposition(comp.getPlaceholderId()).getName();
			}
		}
		return null;
	}

	private void toggleLock(int canonicalId, String name)
	{
		Set<Integer> next = new HashSet<>(locked);
		boolean nowLocked = next.add(canonicalId);
		if (!nowLocked)
		{
			next.remove(canonicalId);
		}
		locked = next;
		saveLocks();

		if (config.chatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				name + (nowLocked ? " is now locked." : " is now unlocked."), null);
		}
	}

	private void notifyBlocked(String message)
	{
		log.debug("Blocked: {}", message);
		if (config.chatMessage())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}
	}

	private void loadLocks()
	{
		String raw = configManager.getRSProfileConfiguration(BankSlotLockConfig.GROUP, BankSlotLockConfig.LOCKED_ITEMS_KEY);
		if (raw == null || raw.isEmpty())
		{
			locked = Collections.emptySet();
			return;
		}

		Set<Integer> ids = new HashSet<>();
		for (String part : raw.split(","))
		{
			try
			{
				ids.add(Integer.parseInt(part.trim()));
			}
			catch (NumberFormatException e)
			{
				log.debug("Skipping bad locked item id '{}'", part);
			}
		}
		locked = ids;
	}

	private void saveLocks()
	{
		if (locked.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(BankSlotLockConfig.GROUP, BankSlotLockConfig.LOCKED_ITEMS_KEY);
			return;
		}

		String raw = locked.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
		configManager.setRSProfileConfiguration(BankSlotLockConfig.GROUP, BankSlotLockConfig.LOCKED_ITEMS_KEY, raw);
	}

	@Provides
	BankSlotLockConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankSlotLockConfig.class);
	}
}
