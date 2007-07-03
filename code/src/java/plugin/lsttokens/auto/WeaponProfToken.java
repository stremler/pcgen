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
package plugin.lsttokens.auto;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import pcgen.base.util.HashMapToList;
import pcgen.cdom.base.AssociatedPrereqObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.base.Constants;
import pcgen.cdom.base.LSTWriteable;
import pcgen.cdom.base.ReferenceUtilities;
import pcgen.cdom.graph.PCGraphGrantsEdge;
import pcgen.core.PObject;
import pcgen.core.WeaponProf;
import pcgen.core.prereq.Prerequisite;
import pcgen.persistence.GraphChanges;
import pcgen.persistence.LoadContext;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.lst.AbstractToken;
import pcgen.persistence.lst.AutoLstToken;
import pcgen.persistence.lst.output.prereq.PrerequisiteWriter;
import pcgen.persistence.lst.utils.TokenUtilities;
import pcgen.util.Logging;

public class WeaponProfToken extends AbstractToken implements AutoLstToken
{

	private static final Class<WeaponProf> WEAPONPROF_CLASS = WeaponProf.class;

	@Override
	public String getTokenName()
	{
		return "WEAPONPROF";
	}

	public boolean parse(PObject target, String value)
	{
		target.addAutoArray(getTokenName(), value);
		return true;
	}

	public boolean parse(LoadContext context, PObject obj, String value)
	{
		String weaponProfs;
		Prerequisite prereq = null; // Do not initialize, null is significant!

		// Note: May contain PRExxx
		if (value.indexOf("[") == -1)
		{
			weaponProfs = value;
		}
		else
		{
			int openBracketLoc = value.indexOf("[");
			weaponProfs = value.substring(0, openBracketLoc);
			if (!value.endsWith("]"))
			{
				Logging.errorPrint("Unresolved Prerequisite in "
					+ getTokenName() + " " + value + " in " + getTokenName());
				return false;
			}
			prereq =
					getPrerequisite(value.substring(openBracketLoc + 1, value
						.length() - 1));
			if (prereq == null)
			{
				Logging.errorPrint("Error generating Prerequisite " + prereq
					+ " in " + getTokenName());
				return false;
			}
		}

		if (hasIllegalSeparator('|', weaponProfs))
		{
			return false;
		}

		boolean foundAny = false;
		boolean foundOther = false;

		StringTokenizer tok = new StringTokenizer(weaponProfs, Constants.PIPE);
		List<CDOMReference<WeaponProf>> refs =
				new ArrayList<CDOMReference<WeaponProf>>();

		while (tok.hasMoreTokens())
		{
			String aProf = tok.nextToken();
			if ("%LIST".equals(value))
			{
				/*
				 * FIXME Need to figure out how to handle this!!!
				 */
				// for (Iterator<AssociatedChoice<String>> e =
				// getAssociatedList()
				// .iterator(); e.hasNext();) {
				// aList.add(e.next().getDefaultChoice());
				// }
			}
			else
			{
				CDOMReference<WeaponProf> ref;
				if (Constants.LST_ANY.equalsIgnoreCase(aProf))
				{
					foundAny = true;
					ref = context.ref.getCDOMAllReference(WEAPONPROF_CLASS);
				}
				else
				{
					foundOther = true;
					ref =
							TokenUtilities.getTypeOrPrimitive(context,
								WEAPONPROF_CLASS, aProf);
				}
				if (ref == null)
				{
					return false;
				}
				refs.add(ref);
			}
		}

		if (foundAny && foundOther)
		{
			Logging.errorPrint("Non-sensical " + getTokenName()
				+ ": Contains ANY and a specific reference: " + value);
			return false;
		}

		for (CDOMReference<WeaponProf> ref : refs)
		{
			PCGraphGrantsEdge edge =
					context.graph.grant(getTokenName(), obj, ref);
			if (prereq != null)
			{
				edge.addPreReq(prereq);
			}
		}

		return true;
	}

	public String[] unparse(LoadContext context, PObject obj)
	{
		GraphChanges<WeaponProf> changes =
				context.graph.getChangesFromToken(getTokenName(), obj,
					WEAPONPROF_CLASS);
		if (changes == null)
		{
			return null;
		}
		Collection<LSTWriteable> added = changes.getAdded();
		if (added == null || added.isEmpty())
		{
			// Zero indicates no Token
			return null;
		}
		HashMapToList<Set<Prerequisite>, LSTWriteable> m =
				new HashMapToList<Set<Prerequisite>, LSTWriteable>();
		for (LSTWriteable ab : added)
		{
			AssociatedPrereqObject assoc = changes.getAddedAssociation(ab);
			m.addToListFor(new HashSet<Prerequisite>(assoc
				.getPrerequisiteList()), ab);
		}

		PrerequisiteWriter prereqWriter = new PrerequisiteWriter();
		SortedSet<LSTWriteable> set =
				new TreeSet<LSTWriteable>(TokenUtilities.WRITEABLE_SORTER);

		String[] array = new String[m.size()];
		int index = 0;

		for (Set<Prerequisite> prereqs : m.getKeySet())
		{
			List<LSTWriteable> profs = m.getListFor(prereqs);
			set.clear();
			set.addAll(profs);
			String ab = ReferenceUtilities.joinLstFormat(set, Constants.PIPE);
			if (prereqs != null && !prereqs.isEmpty())
			{
				if (prereqs.size() > 1)
				{
					context.addWriteMessage("Error: "
						+ obj.getClass().getSimpleName()
						+ " had more than one Prerequisite for "
						+ getTokenName());
					return null;
				}
				Prerequisite p = prereqs.iterator().next();
				StringWriter swriter = new StringWriter();
				try
				{
					prereqWriter.write(swriter, p);
				}
				catch (PersistenceLayerException e)
				{
					context.addWriteMessage("Error writing Prerequisite: " + e);
					return null;
				}
				ab = ab + '[' + swriter.toString() + ']';
			}
			array[index++] = ab;
		}
		return array;
	}

}
