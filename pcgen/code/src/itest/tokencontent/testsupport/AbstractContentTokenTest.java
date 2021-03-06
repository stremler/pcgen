/*
 * Copyright (c) 2012 Tom Parker <thpr@users.sourceforge.net>
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
package tokencontent.testsupport;

import org.junit.Test;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.UserSelection;
import pcgen.cdom.content.CNAbility;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.cdom.helper.ClassSource;
import pcgen.cdom.inst.PCClassLevel;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Campaign;
import pcgen.core.Deity;
import pcgen.core.Domain;
import pcgen.core.EquipmentModifier;
import pcgen.core.PCCheck;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.PCTemplate;
import pcgen.core.Race;
import pcgen.core.character.CompanionMod;
import pcgen.persistence.PersistenceLayerException;
import tokenmodel.testsupport.AbstractTokenModelTest;

public abstract class AbstractContentTokenTest extends AbstractTokenModelTest
{
	@Test
	public void testFromAbility() throws PersistenceLayerException
	{
		Ability source = create(Ability.class, "Source");
		context.ref.reassociateCategory(AbilityCategory.FEAT, source);
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		CNAbilitySelection cas =
				new CNAbilitySelection(new CNAbility(AbilityCategory.FEAT, source,
					Nature.AUTOMATIC));
		directAbilityFacet.add(id, cas, UserSelection.getInstance());
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		directAbilityFacet.remove(id, cas, UserSelection.getInstance());
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromAlignment() throws PersistenceLayerException
	{
		processToken(lg);
		assertEquals(baseCount(), targetFacetCount());
		alignmentFacet.set(id, lg);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		alignmentFacet.set(id, ng);
		assertEquals(baseCount(), targetFacetCount());
	}

	//BioSet not *supposed* to do things like this

	@Test
	public void testFromCampaign() throws PersistenceLayerException
	{
		Campaign source = create(Campaign.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		expandedCampaignFacet.add(id, source, this);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		expandedCampaignFacet.remove(id, source, this);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromCheck() throws PersistenceLayerException
	{
		PCCheck source = create(PCCheck.class, "Source");
		processToken(source);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		checkFacet.remove(id, source);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromClass() throws PersistenceLayerException
	{
		PCClass source = create(PCClass.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		classFacet.addClass(id, source);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		classFacet.removeClass(id, source);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromClassLevel() throws PersistenceLayerException
	{
		PCClassLevel source = create(PCClassLevel.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		classLevelFacet.add(id, source, this);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		classLevelFacet.remove(id, source, this);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromCompanionMod() throws PersistenceLayerException
	{
		CompanionMod source = create(CompanionMod.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		companionModFacet.add(id, source);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		companionModFacet.remove(id, source);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromDeity() throws PersistenceLayerException
	{
		Deity source = create(Deity.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		deityFacet.set(id, source);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		deityFacet.remove(id);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromDomain() throws PersistenceLayerException
	{
		Domain source = create(Domain.class, "Source");
		PCClass pcc = create(PCClass.class, "Class");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		ClassSource classSource = new ClassSource(pcc);
		domainFacet.add(id, source, classSource);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		domainFacet.remove(id, source, classSource);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromEqMod() throws PersistenceLayerException
	{
		EquipmentModifier source = create(EquipmentModifier.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		activeEqModFacet.add(id, source, this);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		activeEqModFacet.remove(id, source, this);
		assertEquals(baseCount(), targetFacetCount());
	}

	//Language not *supposed* to do things like this

	@Test
	public void testFromRace() throws PersistenceLayerException
	{
		Race source = create(Race.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		raceFacet.directSet(id, source, getAssoc());
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		raceFacet.remove(id);
		assertEquals(baseCount(), targetFacetCount());
	}

	//TODO SizeFacet is not a very good model for doing this by hand :(
	//Need to separate the setting of size from the facet that holds it

	//Skill not *supposed* to do things like this

	@Test
	public void testFromStat() throws PersistenceLayerException
	{
		PCStat source = create(PCStat.class, "Source");
		processToken(source);
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		statFacet.remove(id, source);
		assertEquals(baseCount(), targetFacetCount());
	}

	@Test
	public void testFromTemplate() throws PersistenceLayerException
	{
		PCTemplate source = create(PCTemplate.class, "Source");
		processToken(source);
		assertEquals(baseCount(), targetFacetCount());
		templateInputFacet.directAdd(id, source, getAssoc());
		assertTrue(containsExpected());
		assertEquals(baseCount() + 1, targetFacetCount());
		templateInputFacet.remove(id, source);
		assertEquals(baseCount(), targetFacetCount());
	}

	//WeaponProf not *supposed* to do things like this

	protected abstract void processToken(CDOMObject source);

	protected abstract boolean containsExpected();

	protected abstract int targetFacetCount();

	protected abstract int baseCount();

}
