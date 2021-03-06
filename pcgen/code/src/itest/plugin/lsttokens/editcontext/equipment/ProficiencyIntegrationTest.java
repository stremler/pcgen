/*
 * Copyright (c) 2007 Tom Parker <thpr@users.sourceforge.net>
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
package plugin.lsttokens.editcontext.equipment;

import org.junit.Test;

import pcgen.core.ArmorProf;
import pcgen.core.Equipment;
import pcgen.core.ShieldProf;
import pcgen.core.WeaponProf;
import pcgen.persistence.PersistenceLayerException;
import pcgen.rules.persistence.CDOMLoader;
import pcgen.rules.persistence.token.CDOMPrimaryToken;
import plugin.lsttokens.editcontext.testsupport.AbstractIntegrationTestCase;
import plugin.lsttokens.editcontext.testsupport.TestContext;
import plugin.lsttokens.equipment.ProficiencyToken;
import plugin.lsttokens.testsupport.CDOMTokenLoader;

public class ProficiencyIntegrationTest extends
		AbstractIntegrationTestCase<Equipment>
{
	static ProficiencyToken token = new ProficiencyToken();
	static CDOMTokenLoader<Equipment> loader = new CDOMTokenLoader<Equipment>(
			Equipment.class);

	@Override
	public Class<Equipment> getCDOMClass()
	{
		return Equipment.class;
	}

	@Override
	public CDOMLoader<Equipment> getLoader()
	{
		return loader;
	}

	@Override
	public CDOMPrimaryToken<Equipment> getToken()
	{
		return token;
	}

	@Test
	public void testRoundRobinSimple() throws PersistenceLayerException
	{
		primaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		secondaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		primaryContext.ref.constructCDOMObject(ShieldProf.class, "TestWP2");
		secondaryContext.ref.constructCDOMObject(ShieldProf.class, "TestWP2");
		verifyCleanStart();
		TestContext tc = new TestContext();
		commit(testCampaign, tc, "ARMOR|TestWP1");
		commit(modCampaign, tc, "SHIELD|TestWP2");
		completeRoundRobin(tc);
	}

	@Test
	public void testRoundRobinRemove() throws PersistenceLayerException
	{
		primaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		secondaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		primaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP2");
		secondaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP2");
		verifyCleanStart();
		TestContext tc = new TestContext();
		commit(testCampaign, tc, "ARMOR|TestWP1");
		commit(modCampaign, tc, "ARMOR|TestWP2");
		completeRoundRobin(tc);
	}

	@Test
	public void testRoundRobinNoSet() throws PersistenceLayerException
	{
		primaryContext.ref.constructCDOMObject(WeaponProf.class, "TestWP1");
		secondaryContext.ref.constructCDOMObject(WeaponProf.class, "TestWP1");
		verifyCleanStart();
		TestContext tc = new TestContext();
		emptyCommit(testCampaign, tc);
		commit(modCampaign, tc, "WEAPON|TestWP1");
		completeRoundRobin(tc);
	}

	@Test
	public void testRoundRobinNoReset() throws PersistenceLayerException
	{
		primaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		secondaryContext.ref.constructCDOMObject(ArmorProf.class, "TestWP1");
		verifyCleanStart();
		TestContext tc = new TestContext();
		commit(testCampaign, tc, "ARMOR|TestWP1");
		emptyCommit(modCampaign, tc);
		completeRoundRobin(tc);
	}

}
