/**
 * SkillListChoiceManager.java
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
 * Current Version: $Revision: 285 $
 * Last Editor:     $Author: nuance $
 * Last Edited:     $Date: 2006-03-17 15:19:49 +0000 (Fri, 17 Mar 2006) $
 *
 * Copyright 2006 Andrew Wilson <nuance@sourceforge.net>
 */
package pcgen.core.chooser;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import pcgen.core.Ability;
import pcgen.core.Globals;
import pcgen.core.PObject;
import pcgen.core.PlayerCharacter;
import pcgen.core.Skill;

/**
 * This is one of the choosers that deals with choosing a skill.
 */
public class SkillListChoiceManager extends AbstractComplexChoiceManager {

	protected List rootArrayList;

	/**
	 * Make a new Skill List chooser.
	 *
	 * @param aPObject
	 * @param choiceString
	 * @param aPC
	 */
	public SkillListChoiceManager(
			PObject         aPObject,
			String          choiceString,
			PlayerCharacter aPC)
	{
		super(aPObject, choiceString, aPC);
		title = "Skill Choice";
		chooserHandled = "SKILLIST";
		
		if (choices != null && choices.size() > 0 &&
				((String) choices.get(0)).equals(chooserHandled)) {
			choices = choices.subList(1, choices.size());
		}
	}

	/**
	 * Parse the Choice string and build a list of available choices.
	 * @param aPc
	 * @param availableList
	 * @param selectedList
	 */
	public void getChoices(
			final PlayerCharacter aPc,
			final List            availableList,
			final List            selectedList)
	{
		Iterator iter;

		final String choiceVal = choices.get(0) != null
				? (String) choices.get(0)
				: pobject.getName();

		if ((choiceVal.length() > 0) && !"LIST".equals(choiceVal))
		{
			StringTokenizer choiceTok = new StringTokenizer(choiceVal, ",");

			while (choiceTok.hasMoreTokens())
			{
				availableList.add(choiceTok.nextToken());
			}
		}
		
		else // if it was LIST
		{
			Skill aSkill;

			for (iter = Globals.getSkillList().iterator(); iter.hasNext();)
			{
				aSkill = (Skill) iter.next();

				final int rootNameLength = aSkill.getRootName().length();

				 //all skills have ROOTs now, so go ahead and add it if the name and root are identical
				if ((rootNameLength == 0) || aSkill.getRootName().equals(aSkill.getName()))
				{
					availableList.add(aSkill.getName());
				}

				final boolean rootArrayContainsRootName = rootArrayList.contains(aSkill.getRootName());

				if ((rootNameLength > 0) && !rootArrayContainsRootName)
				{
					rootArrayList.add(aSkill.getRootName());
				}

				if ((rootNameLength > 0) && rootArrayContainsRootName)
				{
					availableList.add(aSkill.getName());
				}
			}
		}

		pobject.addAssociatedTo(selectedList);
	}

	/**
	 * Associate a choice with the pobject.
	 * 
	 * @param aPc 
	 * @param item the choice to associate
	 * @param prefix 
	 */
	protected void associateChoice(
			final PlayerCharacter aPc,
			final String          item,
			final String          prefix)
	{
		super.associateChoice(aPc, item, prefix);

		if (pobject != null && pobject instanceof Ability)
		{
			Ability ability = (Ability) pobject;
			if (rootArrayList.contains(item))
			{
				for (Iterator iter = Globals.getSkillList().iterator(); iter.hasNext();)
				{
					final Skill skill = (Skill) iter.next();

					if (skill.getRootName().equalsIgnoreCase(item))
					{
						ability.addCSkill(skill.getName());
					}
				}
			}
			else
			{
				ability.addCSkill(item);
			}
		}
	}

}
