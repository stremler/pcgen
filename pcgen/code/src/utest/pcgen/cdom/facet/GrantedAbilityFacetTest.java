/*
 * Copyright (c) 2009 Tom Parker <thpr@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.cdom.facet;

import pcgen.cdom.content.CNAbility;
import pcgen.cdom.enumeration.CharID;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.facet.base.AbstractSourcedListFacet;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.cdom.testsupport.AbstractSourcedListFacetTest;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;

public class GrantedAbilityFacetTest extends
		AbstractSourcedListFacetTest<CNAbilitySelection>
{
	private GrantedAbilityFacet facet = new GrantedAbilityFacet();

	@Override
	protected AbstractSourcedListFacet<CharID, CNAbilitySelection> getFacet()
	{
		return facet;
	}
	@Override
	protected CNAbilitySelection getObject()
	{
		Ability a1 = new Ability();
		return new CNAbilitySelection(new CNAbility(AbilityCategory.FEAT, a1, Nature.VIRTUAL));
	}

}
