package com.bankslotlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WithdrawAmountTest
{
	@Test
	public void parsesOptions()
	{
		assertEquals(1, WithdrawAmount.parse("Withdraw-1"));
		assertEquals(28, WithdrawAmount.parse("Withdraw-28"));
		assertEquals(WithdrawAmount.ALL, WithdrawAmount.parse("Withdraw-All"));
		assertEquals(WithdrawAmount.ALL_BUT_ONE, WithdrawAmount.parse("Withdraw-All-but-1"));
		assertEquals(WithdrawAmount.UNKNOWN, WithdrawAmount.parse("Withdraw-X"));
		assertEquals(WithdrawAmount.NOT_WITHDRAW, WithdrawAmount.parse("Examine"));
		assertEquals(WithdrawAmount.NOT_WITHDRAW, WithdrawAmount.parse("Release"));
		assertEquals(WithdrawAmount.NOT_WITHDRAW, WithdrawAmount.parse(null));
	}

	@Test
	public void recognisesRememberedX()
	{
		assertTrue(WithdrawAmount.isRememberedX(28));
		assertTrue(WithdrawAmount.isRememberedX(2));
		assertFalse(WithdrawAmount.isRememberedX(1));
		assertFalse(WithdrawAmount.isRememberedX(5));
		assertFalse(WithdrawAmount.isRememberedX(10));
		assertFalse(WithdrawAmount.isRememberedX(WithdrawAmount.ALL));
		assertFalse(WithdrawAmount.isRememberedX(WithdrawAmount.ALL_BUT_ONE));
		assertFalse(WithdrawAmount.isRememberedX(WithdrawAmount.UNKNOWN));
	}

	@Test
	public void decidesWhetherSlotEmpties()
	{
		assertTrue(WithdrawAmount.empties(1, 1, false));
		assertFalse(WithdrawAmount.empties(1, 2, false));
		assertTrue(WithdrawAmount.empties(10, 5, false));
		assertTrue(WithdrawAmount.empties(WithdrawAmount.ALL, 1000, false));
		assertFalse(WithdrawAmount.empties(WithdrawAmount.ALL_BUT_ONE, 1, true));
		assertTrue(WithdrawAmount.empties(WithdrawAmount.UNKNOWN, 1000, true));
		assertFalse(WithdrawAmount.empties(WithdrawAmount.UNKNOWN, 1000, false));
		assertFalse(WithdrawAmount.empties(WithdrawAmount.NOT_WITHDRAW, 1, true));
	}
}
