/*
 * Copyright (c) 2008 Tom Parker <thpr@users.sourceforge.net>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package pcgen.base.formula;

import junit.framework.TestCase;


public class SubtractingFormulaTest extends TestCase
{

	public void testToString()
	{
		assertEquals("-1", new SubtractingFormula(1).toString());
		assertEquals("-3", new SubtractingFormula(3).toString());
		assertEquals("+3", new SubtractingFormula(-3).toString());
		assertEquals("-0", new SubtractingFormula(0).toString());
	}
	
	public void testIdentity()
	{
		SubtractingFormula f = new SubtractingFormula(1);
		assertEquals(-1, f.resolve(Integer.valueOf(0)).intValue());
		assertEquals(1, f.resolve(Integer.valueOf(2)).intValue());
		assertEquals(1, f.resolve(Double.valueOf(2.5)).intValue());
		testBrokenCalls(f);
	}

	public void testEquality()
	{
		SubtractingFormula f1 = new SubtractingFormula(1);
		SubtractingFormula f2 = new SubtractingFormula(1);
		SubtractingFormula f3 = new SubtractingFormula(2);
		SubtractingFormula f4 = new SubtractingFormula(-1);
		assertTrue(f1 != f2);
		assertEquals(f1.hashCode(), f2.hashCode());
		assertEquals(f1, f2);
		assertFalse(f1.equals(null));
		assertFalse(f1.hashCode() == f3.hashCode());
		assertFalse(f1.equals(f3));
		assertFalse(f1.hashCode() == f4.hashCode());
		assertFalse(f1.equals(f4));
	}

	public void testPositive()
	{
		SubtractingFormula f = new SubtractingFormula(3);
		assertEquals(2, f.resolve(Integer.valueOf(5)).intValue());
		assertEquals(2, f.resolve(Double.valueOf(5.5)).intValue());
		testBrokenCalls(f);
	}

	public void testZero()
	{
		SubtractingFormula f = new SubtractingFormula(0);
		assertEquals(5, f.resolve(Integer.valueOf(5)).intValue());
		assertEquals(2, f.resolve(Double.valueOf(2.3)).intValue());
		testBrokenCalls(f);
	}

	public void testNegative()
	{
		SubtractingFormula f = new SubtractingFormula(-2);
		assertEquals(7, f.resolve(Integer.valueOf(5)).intValue());
		assertEquals(-4, f.resolve(Double.valueOf(-6.7)).intValue());
		testBrokenCalls(f);
	}

	private void testBrokenCalls(SubtractingFormula f)
	{
		try
		{
			f.resolve((Number[]) null);
			fail("null should be illegal");
		}
		catch (IllegalArgumentException e)
		{
			// OK
		}
		try
		{
			f.resolve(new Number[]{});
			fail("empty array should be illegal");
		}
		catch (IllegalArgumentException e)
		{
			// OK
		}
		try
		{
			f.resolve(new Number[]{Integer.valueOf(4), Double.valueOf(2.5)});
			fail("two arguments in array should be illegal");
		}
		catch (IllegalArgumentException e)
		{
			// OK
		}
		try
		{
			f.resolve(Integer.valueOf(4), Double.valueOf(2.5));
			fail("two arguments should be illegal");
		}
		catch (IllegalArgumentException e)
		{
			// OK
		}
	}

}
