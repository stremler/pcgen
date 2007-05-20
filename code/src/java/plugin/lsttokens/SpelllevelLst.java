/*
 * Copyright 2006-2007 (C) Tom Parker <thpr@users.sourceforge.net>
 * Copyright 2005-2006 (C) Devon Jones
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
 *
 * Current Ver: $Revision$
 * Last Editor: $Author$
 * Last Edited: $Date$
 */
package plugin.lsttokens;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import pcgen.base.lang.StringUtil;
import pcgen.base.util.DoubleKeyMapToList;
import pcgen.base.util.HashMapToList;
import pcgen.cdom.base.AssociatedPrereqObject;
import pcgen.cdom.base.CDOMList;
import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.base.Constants;
import pcgen.cdom.base.LSTWriteable;
import pcgen.cdom.enumeration.AssociationKey;
import pcgen.cdom.enumeration.SkillCost;
import pcgen.cdom.graph.PCGraphEdge;
import pcgen.cdom.inst.Aggregator;
import pcgen.cdom.util.ReferenceUtilities;
import pcgen.core.Campaign;
import pcgen.core.PObject;
import pcgen.core.SpellList;
import pcgen.core.WeaponProf;
import pcgen.core.WeaponProfList;
import pcgen.core.prereq.Prerequisite;
import pcgen.core.spell.Spell;
import pcgen.persistence.GraphChanges;
import pcgen.persistence.LoadContext;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.lst.AbstractToken;
import pcgen.persistence.lst.GlobalLstToken;
import pcgen.persistence.lst.output.prereq.PrerequisiteWriter;
import pcgen.persistence.lst.prereq.PreParserFactory;
import pcgen.persistence.lst.utils.TokenUtilities;
import pcgen.util.Logging;

/**
 * @author djones4
 * 
 */
public class SpelllevelLst extends AbstractToken implements GlobalLstToken
{

	@Override
	public String getTokenName()
	{
		return "SPELLLEVEL";
	}

	public boolean parse(PObject obj, String value, int anInt)
	{
		// SPELLLEVEL:CLASS|Name1,Name2=Level1|Spell1,Spell2,Spell3|Name3=Level2|Spell4,Spell5|PRExxx|PRExxx
		if (!(obj instanceof Campaign))
		{
			final StringTokenizer tok = new StringTokenizer(value, "|");

			if (tok.countTokens() < 3)
			{
				Logging.errorPrint("Badly formed SPELLLEVEL tag1: " + value);
				return false;
			}

			final String tagType = tok.nextToken(); // CLASS or DOMAIN
			final List<String> preList = new ArrayList<String>();

			// The 2 lists below should always have the same number of items
			final List<String> wNameList = new ArrayList<String>();
			final List<String> wSpellList = new ArrayList<String>();

			while (tok.hasMoreTokens())
			{
				final String nameList = tok.nextToken();

				if (nameList.startsWith("PRE") || nameList.startsWith("!PRE"))
				{
					preList.add(nameList);
					break;
				}

				if (nameList.indexOf("=") < 0)
				{
					Logging
						.errorPrint("Badly formed SPELLLEVEL tag2: " + value);
					return false;
				}

				wNameList.add(nameList);

				if (!tok.hasMoreTokens())
				{
					Logging
						.errorPrint("Badly formed SPELLLEVEL tag3: " + value);
					return false;
				}

				wSpellList.add(tok.nextToken());
			}

			while (tok.hasMoreTokens())
			{
				final String nameList = tok.nextToken();

				if (nameList.startsWith("PRE") || nameList.startsWith("!PRE"))
				{
					preList.add(nameList);
				}
				else
				{
					Logging.errorPrint("Badly formed SPELLLEVEL PRE tag: "
						+ value);
					return false;
				}
			}

			//
			// Parse the prereq list
			//
			List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
			try
			{
				PreParserFactory factory = PreParserFactory.getInstance();
				prereqs = factory.parse(preList);
			}
			catch (PersistenceLayerException ple)
			{
				Logging.errorPrint("Badly formed SPELLLEVEL PRE tag: " + value);
			}

			for (Iterator<String> iSpell = wSpellList.iterator(), iName =
					wNameList.iterator(); iSpell.hasNext() || iName.hasNext();)
			{
				// Check to see if both exists
				if (!(iSpell.hasNext() && iName.hasNext()))
				{
					Logging
						.errorPrint("Badly formed SPELLLEVEL tag4: " + value);
					return false;
				}

				final StringTokenizer bTok =
						new StringTokenizer(iSpell.next(), ",");
				final String classList = iName.next();

				while (bTok.hasMoreTokens())
				{
					final String spellLevel =
							classList.substring(classList.indexOf("=") + 1);
					final String spellName = bTok.nextToken();
					final StringTokenizer cTok =
							new StringTokenizer(classList.substring(0,
								classList.indexOf("=")), ",");

					while (cTok.hasMoreTokens())
					{
						final String className = cTok.nextToken();

						if (className.startsWith("SPELLCASTER.")
							|| !obj.getSpellSupport().containsLevelFor(tagType,
								className, spellName))
						{
							obj.getSpellSupport().addSpellLevel(tagType,
								className, spellName, spellLevel, prereqs);
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	public boolean parse(LoadContext context, CDOMObject obj, String value)
	{
		// SPELLLEVEL:CLASS|Name1,Name2=Level1|Spell1,Spell2,Spell3|Name3=Level2|Spell4,Spell5|PRExxx|PRExxx

		if (isEmpty(value) || hasIllegalSeparator('|', value))
		{
			return false;
		}

		String workingValue = value;
		List<Prerequisite> prereqs = new ArrayList<Prerequisite>();
		while (true)
		{
			int lastPipeLoc = workingValue.lastIndexOf('|');
			if (lastPipeLoc == -1)
			{
				Logging.errorPrint("Invalid " + getTokenName()
					+ " not enough tokens");
				return false;
			}
			String lastToken = workingValue.substring(lastPipeLoc + 1);
			if (lastToken.startsWith("PRE") || lastToken.startsWith("!PRE"))
			{
				workingValue = workingValue.substring(0, lastPipeLoc);
				prereqs.add(getPrerequisite(lastToken));
			}
			else
			{
				break;
			}
		}

		StringTokenizer tok = new StringTokenizer(workingValue, "|");

		if (tok.countTokens() < 3)
		{
			Logging.errorPrint("Insufficient values in SPELLLEVEL tag: "
				+ value);
			return false;
		}

		String tagType = tok.nextToken(); // CLASS or DOMAIN

		while (tok.hasMoreTokens())
		{
			String tokString = tok.nextToken();
			String spellString = tok.nextToken();

			if (!subParse(context, obj, tagType, tokString, spellString,
				prereqs))
			{
				Logging.errorPrint("  " + getTokenName()
					+ " error - entire token was " + value);
				return false;
			}
		}

		return true;
	}

	private boolean subParse(LoadContext context, CDOMObject obj,
		String tagType, String tokString, String spellString,
		List<Prerequisite> prereqs)
	{
		int equalLoc = tokString.indexOf(Constants.EQUALS);
		if (equalLoc == -1)
		{
			Logging.errorPrint("Expected an = in SPELLLEVEL " + "definition: "
				+ tokString);
			return false;
		}

		String casterString = tokString.substring(0, equalLoc);
		String spellLevel = tokString.substring(equalLoc + 1);
		Integer splLevel;
		try
		{
			splLevel = Integer.decode(spellLevel);
		}
		catch (NumberFormatException nfe)
		{
			Logging.errorPrint("Expected a number for SPELLLEVEL, found: "
				+ spellLevel);
			return false;
		}

		if (hasIllegalSeparator(',', casterString))
		{
			return false;
		}

		StringTokenizer clTok =
				new StringTokenizer(casterString, Constants.COMMA);
		List<CDOMReference<SpellList>> slList =
				new ArrayList<CDOMReference<SpellList>>();
		while (clTok.hasMoreTokens())
		{
			String classString = clTok.nextToken();
			if (classString.startsWith("SPELLCASTER."))
			{
				/*
				 * TODO Deprecate the use of SPELLCASTER? No need for it, since
				 * the function isn't any different.
				 * 
				 * Actually, I think it IS necessary still - thpr 1/8/07
				 */
				classString = classString.substring(12);
			}
			if (classString.length() == 0)
			{
				Logging
					.errorPrint("Cannot resolve empty SpellList reference in "
						+ getTokenName());
				return false;
			}
			slList.add(context.ref.getCDOMReference(SpellList.class,
				classString));
		}

		if (hasIllegalSeparator(',', spellString))
		{
			return false;
		}

		StringTokenizer spTok = new StringTokenizer(spellString, ",");

		while (spTok.hasMoreTokens())
		{
			String spellName = spTok.nextToken();
			CDOMReference<Spell> sp =
					context.ref.getCDOMReference(Spell.class, spellName);
			for (CDOMReference<SpellList> sl : slList)
			{
				AssociatedPrereqObject tpr =
						context.list.addToList(getTokenName(), obj, sl, sp);
				tpr.setAssociation(AssociationKey.SPELL_LEVEL, splLevel);
				tpr.setAssociation(AssociationKey.TYPE, tagType);
				tpr.addAllPrerequisites(prereqs);
			}
		}
		return true;
	}

	public String[] unparse(LoadContext context, CDOMObject obj)
	{
		Collection<CDOMReference<CDOMList<? extends CDOMObject>>> changedLists =
				context.list.getChangedLists(obj, SpellList.class);

		DoubleKeyMapToList<String, Set<Prerequisite>, String> m =
				new DoubleKeyMapToList<String, Set<Prerequisite>, String>();
		for (CDOMReference listRef : changedLists)
		{
			GraphChanges<WeaponProf> changes =
					context.list.getChangesInList(getTokenName(), obj, listRef);
			if (changes == null)
			{
				// Legal if no SPELLLEVEL was present
				continue;
			}
			if (changes.hasRemovedItems() || changes.includesGlobalClear())
			{
				context.addWriteMessage(getTokenName()
					+ " does not support .CLEAR");
				return null;
			}
			if (changes.hasAddedItems())
			{
				Collection<LSTWriteable> addedCollection = changes.getAdded();
				HashMapToList<Integer, LSTWriteable> hml =
						new HashMapToList<Integer, LSTWriteable>();
				for (LSTWriteable added : addedCollection)
				{
					AssociatedPrereqObject se =
							changes.getAddedAssociation(added);
					String type = se.getAssociation(AssociationKey.TYPE);
					if (type == null)
					{
						context.addWriteMessage("Invalid " + getTokenName()
							+ " link: has no TYPE");
						return null;
					}
					Integer lvl = se.getAssociation(AssociationKey.SPELL_LEVEL);
					hml.addToListFor(lvl, added);
					StringBuilder sb = new StringBuilder();
				}
				SortedSet<LSTWriteable> spSet =
						new TreeSet<LSTWriteable>(
							TokenUtilities.WRITEABLE_SORTER);
				StringBuilder sb = new StringBuilder();
				for (Integer lvl : new TreeSet<Integer>(hml.getKeySet()))
				{
					sb.setLength(0);
					spSet.clear();
					spSet.addAll(hml.getListFor(lvl));
					sb.append(ReferenceUtilities.joinLstFormat(slSet,
						Constants.COMMA));
					sb.append('=').append(lvl).append(Constants.PIPE);
					sb.append(ReferenceUtilities.joinLstFormat(spSet,
						Constants.COMMA));
					m.addToListFor(type, new HashSet<Prerequisite>(agg
						.getPrerequisiteList()), sb.toString());
				}
			}
			return list.toArray(new String[list.size()]);

			PrerequisiteWriter prereqWriter = new PrerequisiteWriter();
			Set<String> list = new TreeSet<String>();
			for (String type : m.getKeySet())
			{
				for (Set<Prerequisite> prereqs : m.getSecondaryKeySet(type))
				{
					StringBuilder sb = new StringBuilder();
					Set<String> set =
							new TreeSet<String>(m.getListFor(type, prereqs));
					sb.append(StringUtil.join(set, Constants.PIPE));
					if (prereqs != null && !prereqs.isEmpty())
					{
						for (Prerequisite p : prereqs)
						{
							StringWriter swriter = new StringWriter();
							try
							{
								prereqWriter.write(swriter, p);
							}
							catch (PersistenceLayerException e)
							{
								context
									.addWriteMessage("Error writing Prerequisite: "
										+ e);
								return null;
							}
							sb.append(Constants.PIPE)
								.append(swriter.toString());
						}
					}
					list.add(type + "|" + sb.toString());
				}
			}
			return list.toArray(new String[list.size()]);
		}
	}
}
