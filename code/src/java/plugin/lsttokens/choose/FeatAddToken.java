/*
 * Copyright 2007 (C) Thomas Parker <thpr@users.sourceforge.net>
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
package plugin.lsttokens.choose;

import java.util.StringTokenizer;

import pcgen.cdom.base.CDOMEdgeReference;
import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.enumeration.AbilityCategory;
import pcgen.cdom.enumeration.AbilityNature;
import pcgen.cdom.enumeration.AssociationKey;
import pcgen.cdom.factory.GrantFactory;
import pcgen.cdom.helper.ChoiceSet;
import pcgen.cdom.helper.PrimitiveChoiceSet;
import pcgen.core.Ability;
import pcgen.core.ArmorProf;
import pcgen.core.Constants;
import pcgen.core.PObject;
import pcgen.persistence.LoadContext;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.lst.AbstractToken;
import pcgen.persistence.lst.ChooseCompatibilityToken;
import pcgen.persistence.lst.ChooseLoader;
import pcgen.persistence.lst.ChooseLstToken;
import pcgen.util.Logging;

public class FeatAddToken extends AbstractToken implements ChooseLstToken,
		ChooseCompatibilityToken
{

	public boolean parse(PObject po, String prefix, String value)
	{
		if (value == null)
		{
			Logging.errorPrint("CHOOSE:" + getTokenName()
				+ " requires additional arguments");
			return false;
		}
		if (value.indexOf(',') != -1)
		{
			Logging.errorPrint("CHOOSE:" + getTokenName()
				+ " arguments may not contain , : " + value);
			return false;
		}
		if (value.indexOf('[') != -1)
		{
			Logging.errorPrint("CHOOSE:" + getTokenName()
				+ " arguments may not contain [] : " + value);
			return false;
		}
		if (hasIllegalSeparator('|', value))
		{
			return false;
		}

		StringTokenizer st = new StringTokenizer(value, Constants.PIPE);
		while (st.hasMoreTokens())
		{
			String tokString = st.nextToken();
			int equalsLoc = tokString.indexOf("=");
			if (equalsLoc == tokString.length() - 1)
			{
				Logging.errorPrint("CHOOSE:" + getTokenName()
					+ " arguments must have value after = : " + tokString);
				Logging.errorPrint("  entire token was: " + value);
				return false;
			}
		}
		StringBuilder sb = new StringBuilder();
		if (prefix.length() > 0)
		{
			sb.append(prefix).append('|');
		}
		sb.append(getTokenName()).append('|').append(value);
		po.setChoiceString(sb.toString());
		return true;
	}

	@Override
	public String getTokenName()
	{
		return "FEATADD";
	}

	public int compatibilityLevel()
	{
		return 5;
	}

	public int compatibilityPriority()
	{
		return 0;
	}

	public int compatibilitySubLevel()
	{
		return 14;
	}

	public PrimitiveChoiceSet<?> parse(LoadContext context, CDOMObject cdo,
		String value) throws PersistenceLayerException
	{
		// TODO Need to tell the parser the CATEGORY is FEAT
		PrimitiveChoiceSet<Ability> pcs =
				ChooseLoader.parseToken(context, Ability.class, "QUALIFIED["
					+ value + "]");
		CDOMEdgeReference assocref =
				context.graph.getEdgeReference(cdo, ChoiceSet.class, "CHOOSE",
					Ability.class);
		GrantFactory<ArmorProf> gf = new GrantFactory<ArmorProf>(assocref);

		/*
		 * FUTURE Technically, this Category item should not be in the
		 * GrantFactory, as it really belogs as something that can be extracted
		 * from the ChoiceSet...
		 */
		gf
			.setAssociation(AssociationKey.ABILITY_CATEGORY,
				AbilityCategory.FEAT);
		gf.setAssociation(AssociationKey.ABILITY_NATURE, AbilityNature.NORMAL);
		context.graph.grant(getTokenName(), cdo, gf);
		return pcs;
	}
}
