package com.bankslotlock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works out how many items a bank menu option withdraws, from its text.
 */
final class WithdrawAmount
{
	/** The option is not a withdraw. */
	static final int NOT_WITHDRAW = -1;
	/** Withdraw-X: the amount is typed after the click, so it is unknown here. */
	static final int UNKNOWN = -2;
	/** Withdraw-All: always takes the whole stack. */
	static final int ALL = Integer.MAX_VALUE;
	/** Withdraw-All-but-1: never empties the slot. */
	static final int ALL_BUT_ONE = 0;

	private static final Pattern NUMBER = Pattern.compile("withdraw-(\\d+)");

	private WithdrawAmount()
	{
	}

	static int parse(String option)
	{
		if (option == null)
		{
			return NOT_WITHDRAW;
		}

		String o = option.trim().toLowerCase();
		if (!o.startsWith("withdraw-"))
		{
			return NOT_WITHDRAW;
		}
		if (o.equals("withdraw-all"))
		{
			return ALL;
		}
		if (o.equals("withdraw-all-but-1"))
		{
			return ALL_BUT_ONE;
		}
		if (o.equals("withdraw-x"))
		{
			return UNKNOWN;
		}

		Matcher m = NUMBER.matcher(o);
		if (m.matches())
		{
			try
			{
				return Integer.parseInt(m.group(1));
			}
			catch (NumberFormatException e)
			{
				return ALL;
			}
		}
		return UNKNOWN;
	}

	/**
	 * Whether a numeric withdraw is the remembered Withdraw-X amount rather than one of the
	 * game's fixed Withdraw-1/5/10 options.
	 */
	static boolean isRememberedX(int amount)
	{
		return amount > 0 && amount != ALL && amount != 1 && amount != 5 && amount != 10;
	}

	/**
	 * Whether this withdraw would leave the slot empty.
	 *
	 * @param amount   result of {@link #parse}
	 * @param quantity current stack size in the bank
	 * @param blockUnknown treat Withdraw-X as emptying
	 */
	static boolean empties(int amount, int quantity, boolean blockUnknown)
	{
		if (amount == NOT_WITHDRAW || amount == ALL_BUT_ONE)
		{
			return false;
		}
		if (amount == UNKNOWN)
		{
			return blockUnknown;
		}
		return amount >= quantity;
	}
}
