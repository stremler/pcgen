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
package pcgen.rules.persistence.token;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.base.SelectableSet;
import pcgen.cdom.base.ChooseSelectionActor;
import pcgen.cdom.base.Constants;
import pcgen.cdom.base.PersistentChoiceActor;
import pcgen.cdom.base.PersistentTransitionChoice;
import pcgen.cdom.base.PrimitiveChoiceSet;
import pcgen.cdom.base.ChoiceSet;
import pcgen.cdom.choiceset.ReferenceChoiceSet;
import pcgen.cdom.enumeration.AssociationListKey;
import pcgen.cdom.enumeration.ListKey;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.rules.context.LoadContext;
import pcgen.rules.persistence.TokenUtilities;

public abstract class AbstractSimpleChooseToken<T extends CDOMObject> extends
		AbstractTokenWithSeparator<CDOMObject> implements CDOMSecondaryToken<CDOMObject>,
		PersistentChoiceActor<T>
{
	public String getParentToken()
	{
		return "CHOOSE";
	}

	@Override
	protected char separator()
	{
		return '|';
	}

	@Override
	protected ParseResult parseTokenWithSeparator(LoadContext context,
		CDOMObject obj, String value)
	{
		int pipeLoc = value.indexOf('|');
		String activeValue;
		String title;
		if (pipeLoc == -1)
		{
			activeValue = value;
			title = getDefaultTitle();
		}
		else
		{
			String titleString = value.substring(pipeLoc + 1);
			if (titleString.startsWith("TITLE="))
			{
				title = titleString.substring(6);
				if (title.startsWith("\""))
				{
					title = title.substring(1, title.length() - 1);
				}
				activeValue = value.substring(0, pipeLoc);
			}
			else
			{
				activeValue = value;
				title = getDefaultTitle();
			}
		}
		Set<CDOMReference<T>> set = new HashSet<CDOMReference<T>>();
		if (Constants.LST_ALL.equals(activeValue))
		{
			set.add(context.ref.getCDOMAllReference(getChooseClass()));
		}
		else
		{
			StringTokenizer st = new StringTokenizer(activeValue, ",");
			while (st.hasMoreTokens())
			{
				String tok = st.nextToken();
				CDOMReference<T> ref = TokenUtilities.getTypeOrPrimitive(
						context, getChooseClass(), tok);
				if (ref == null)
				{
					return new ParseResult.Fail("Error: Count not get Reference for " + tok
									+ " in " + getTokenName());
				}
				if (!set.add(ref))
				{
					return new ParseResult.Fail("Error, Found item: " + ref
							+ " twice while parsing " + getTokenName());
				}
			}
			if (set.isEmpty())
			{
				return new ParseResult.Fail("No items in set.");
			}
		}
		PrimitiveChoiceSet<T> pcs = new ReferenceChoiceSet<T>(set);

		if (!pcs.getGroupingState().isValid())
		{
			ComplexParseResult cpr = new ComplexParseResult();
			cpr.addErrorMessage("Invalid combination of objects was used in: "
					+ activeValue);
			cpr.addErrorMessage("  Check that ALL is not combined");
			cpr.addErrorMessage("  Check that a key is not joined with AND (,)");
			return cpr;
		}
		ChoiceSet<T> cs = new ChoiceSet<T>(getTokenName(), pcs);
		cs.setTitle(title);
		PersistentTransitionChoice<T> tc = new PersistentTransitionChoice<T>(
				cs, null);
		tc.setChoiceActor(this);
		context.obj.put(obj, ObjectKey.CHOOSE_INFO, tc);
		return ParseResult.SUCCESS;
	}

	public Class<CDOMObject> getTokenClass()
	{
		return CDOMObject.class;
	}

	public String[] unparse(LoadContext context, CDOMObject cdo)
	{
		PersistentTransitionChoice<?> tc = context.getObjectContext()
				.getObject(cdo, ObjectKey.CHOOSE_INFO);
		if (tc == null)
		{
			return null;
		}
		SelectableSet<?> choices = tc.getChoices();
		if (!choices.getName().equals(getTokenName()))
		{
			// Don't unparse anything that isn't owned by this SecondaryToken
			/*
			 * TODO Either this really needs to be a check against the subtoken
			 * (which thus needs to be stored in the ChooseInfo) or there needs
			 * to be a loadtime check that no more than once CHOOSE subtoken
			 * uses the same AssociationListKey... :P
			 */
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(choices.getLSTformat());
		String title = choices.getTitle();
		if (!title.equals(getDefaultTitle()))
		{
			sb.append("|TITLE=");
			sb.append(title);
		}
		return new String[] { sb.toString() };
	}

	public void applyChoice(CDOMObject owner, T st, PlayerCharacter pc)
	{
		restoreChoice(pc, owner, st);
		List<ChooseSelectionActor<?>> actors = owner
				.getListFor(ListKey.NEW_CHOOSE_ACTOR);
		if (actors != null)
		{
			for (ChooseSelectionActor ca : actors)
			{
				applyChoice(owner, st, pc, ca);
			}
		}
		pc.addAssociation(owner, encodeChoice(st));
	}

	private void applyChoice(CDOMObject owner, T st, PlayerCharacter pc,
			ChooseSelectionActor ca)
	{
		ca.applyChoice(owner, st, pc);
	}

	public void removeChoice(PlayerCharacter pc, CDOMObject owner, T choice)
	{
		pc.removeAssoc(owner, getListKey(), choice);
		List<ChooseSelectionActor<?>> actors = owner
				.getListFor(ListKey.NEW_CHOOSE_ACTOR);
		if (actors != null)
		{
			for (ChooseSelectionActor ca : actors)
			{
				ca.removeChoice(owner, choice, pc);
			}
		}
	}

	public void restoreChoice(PlayerCharacter pc, CDOMObject owner, T choice)
	{
		pc.addAssoc(owner, getListKey(), choice);
	}

	public List<T> getCurrentlySelected(CDOMObject owner, PlayerCharacter pc)
	{
		return pc.getAssocList(owner, getListKey());
	}

	public boolean allow(T choice, PlayerCharacter pc, boolean allowStack)
	{
		/*
		 * This is universally true, as any filter for qualify, etc. was dealt
		 * with by the ChoiceSet built during parse
		 */
		return true;
	}

	public T decodeChoice(String s)
	{
		return Globals.getContext().ref.silentlyGetConstructedCDOMObject(
				getChooseClass(), s);
	}

	public String encodeChoice(T choice)
	{
		return choice.getKeyName();
	}

	protected abstract Class<T> getChooseClass();

	protected abstract String getDefaultTitle();

	protected abstract AssociationListKey<T> getListKey();
}
