/**
 * CharacterAbilities.java
 * Copyright James Dempsey, 2011
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
 * Created on 19/03/2011 12:06:34 PM
 *
 * $Id$
 */
package pcgen.gui2.facade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import pcgen.cdom.base.CDOMObjectUtilities;
import pcgen.cdom.base.Category;
import pcgen.cdom.base.Constants;
import pcgen.cdom.content.CNAbility;
import pcgen.cdom.enumeration.CharID;
import pcgen.cdom.enumeration.Nature;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.facet.CategorizedDataFacetChangeEvent;
import pcgen.cdom.facet.DirectAbilityFacet;
import pcgen.cdom.facet.FacetLibrary;
import pcgen.cdom.facet.GrantedAbilityFacet;
import pcgen.cdom.facet.event.DataFacetChangeEvent;
import pcgen.cdom.facet.event.DataFacetChangeListener;
import pcgen.cdom.facet.input.ActiveAbilityFacet;
import pcgen.cdom.helper.CNAbilitySelection;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.AbilityUtilities;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.RuleConstants;
import pcgen.core.chooser.ChooserUtilities;
import pcgen.core.display.CharacterDisplay;
import pcgen.core.facade.AbilityCategoryFacade;
import pcgen.core.facade.AbilityFacade;
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.core.facade.event.ChangeEvent;
import pcgen.core.facade.event.ChangeListener;
import pcgen.core.facade.util.DefaultListFacade;
import pcgen.core.facade.util.ListFacade;
import pcgen.core.utils.MessageType;
import pcgen.core.utils.ShowMessageDelegate;
import pcgen.system.LanguageBundle;
import pcgen.util.Logging;
import pcgen.util.enumeration.Tab;
import pcgen.util.enumeration.View;

/**
 * The Class <code>CharacterAbilities</code> manages the interaction between 
 * the core and the user interface for abilities. It listens for changes in 
 * abilities in the core and then updates the lists provided to the UI to
 * reflect the changes. The lists automatically notify any listeners in the 
 * UI of the changes.
 *   
 * <br/>
 * 
 * Last Editor: $Author$
 * Last Edited: $Date$
 * 
 * @author James Dempsey <jdempsey@users.sourceforge.net>
 * @version $Revision$
 */
public class CharacterAbilities
{

	private final PlayerCharacter theCharacter;
	private final CharacterDisplay charDisplay;
	private UIDelegate delegate;

	private Map<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>> abilityListMap;
	private DefaultListFacade<AbilityCategoryFacade> activeCategories;
	private CharID charID;
	private DataSetFacade dataSetFacade;
	private final List<ChangeListener> abilityCatSelectionListeners;
	private final TodoManager todoManager;
	private ActiveAbilityChangeHandler activeAbilityChangeHandler;
	private GrantedAbilityChangeHandler grantedAbilityChangeHandler;
	private DirectAbilityChangeHandler directAbilityChangeHandler;

	/**
	 * Create a new instance of CharacterAbilities for a character.
	 * @param pc The character we are tracking abilities for.
	 * @param delegate The user interface delegate for notifying the user.
	 * @param dataSetFacade The datasets that the character is using.
	 * @param todoManager The user tasks tracker.
	 */
	public CharacterAbilities(PlayerCharacter pc, UIDelegate delegate,
		DataSetFacade dataSetFacade, TodoManager todoManager)
	{
		theCharacter = pc;
		charDisplay = pc.getDisplay();
		this.delegate = delegate;
		this.dataSetFacade = dataSetFacade;
		this.todoManager = todoManager;
		abilityCatSelectionListeners = new ArrayList<ChangeListener>();
		
		initForCharacter();
	}

	/**
	 * Tidy up character listeners when closing the character. 
	 */
	protected void closeCharacter()
	{
		DirectAbilityFacet directAbilityFacet = FacetLibrary.getFacet(DirectAbilityFacet.class);
		directAbilityFacet.removeDataFacetChangeListener(directAbilityChangeHandler);
		GrantedAbilityFacet grantedAbilityFacet = FacetLibrary.getFacet(GrantedAbilityFacet.class);
		grantedAbilityFacet.removeDataFacetChangeListener(grantedAbilityChangeHandler);
		ActiveAbilityFacet activeAbilityFacet = FacetLibrary.getFacet(ActiveAbilityFacet.class);
		activeAbilityFacet.removeDataFacetChangeListener(activeAbilityChangeHandler);
	}
	
	private void initForCharacter()
	{
		abilityListMap = 
				new LinkedHashMap<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>>();
		activeCategories = new DefaultListFacade<AbilityCategoryFacade>();

		charID = theCharacter.getCharID();
		GrantedAbilityFacet grantedAbilityFacet = FacetLibrary.getFacet(GrantedAbilityFacet.class);
		DirectAbilityFacet directAbilityFacet = FacetLibrary.getFacet(DirectAbilityFacet.class);
		ActiveAbilityFacet activeAbilityFacet = FacetLibrary.getFacet(ActiveAbilityFacet.class);
		
		//theCharacter.getAbilityList(cat, nature)
		rebuildAbilityLists();
		
		activeAbilityChangeHandler = new ActiveAbilityChangeHandler();
		activeAbilityFacet.addDataFacetChangeListener(activeAbilityChangeHandler);
		grantedAbilityChangeHandler = new GrantedAbilityChangeHandler();
		grantedAbilityFacet.addDataFacetChangeListener(grantedAbilityChangeHandler);
		directAbilityChangeHandler = new DirectAbilityChangeHandler();
		directAbilityFacet.addDataFacetChangeListener(directAbilityChangeHandler);
	}

	private void addAbilityToLists(AbilityCategory cat, Ability ability, Nature nature)
	{
		addCategorisedAbility(cat, ability, nature, abilityListMap);
		if (!activeCategories.containsElement(cat))
		{
			int index = getCatIndex(cat, activeCategories);
			activeCategories.addElement(index, cat);
		}
		else
		{
			adviseSelectionChangeLater(cat);				
		}
		updateAbilityCategoryLater(cat);		
	}

	void removeAbilityFromLists(AbilityCategory cat,
		Ability ability, Nature nature)
	{
		removeCategorisedAbility(cat, ability, nature);
		
		boolean stillActive = cat.isVisibleTo(View.VISIBLE_DISPLAY);
		if (!stillActive && activeCategories.containsElement(cat))
		{
			activeCategories.removeElement(cat);
		}
		else
		{
			adviseSelectionChangeLater(cat);				
		}
		updateAbilityCategoryLater(cat);		
	}
	
	/**
	 * Rebuild the ability lists for the character to include the character's 
	 * current abilities.
	 */
	synchronized void rebuildAbilityLists()
	{
		Map<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>> workingAbilityListMap =
				new LinkedHashMap<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>>();
		DefaultListFacade<AbilityCategoryFacade> workingActiveCategories = new DefaultListFacade<AbilityCategoryFacade>();

		ListFacade<AbilityCategoryFacade> categories = dataSetFacade.getAbilityCategories();
		for (AbilityCategoryFacade category : categories)
		{
			AbilityCategory cat = (AbilityCategory) category;

			for (CNAbility cna : theCharacter.getPoolAbilities(cat))
			{
				addCategorisedAbility(cat, cna.getAbility(), cna.getNature(), workingAbilityListMap);
			}

			// deal with visibility
			boolean visible = cat.isVisibleTo(theCharacter, View.VISIBLE_DISPLAY);

			if (visible && !workingActiveCategories.containsElement(cat))
			{
				int index = getCatIndex(cat, workingActiveCategories);
				workingActiveCategories.addElement(index, cat);
			}
			if (!visible && workingActiveCategories.containsElement(cat))
			{
				workingActiveCategories.removeElement(cat);
//				updateAbilityCategoryTodo(cat);
			}
			
			if (visible)
			{
				adviseSelectionChangeLater(cat);
			}
		}
		
		// Update map contents
		for (AbilityCategoryFacade category : workingAbilityListMap.keySet())
		{
			DefaultListFacade<AbilityFacade> workingListFacade = workingAbilityListMap.get(category);
			DefaultListFacade<AbilityFacade> masterListFacade = abilityListMap.get(category);
			if (masterListFacade == null)
			{
				abilityListMap.put(category, workingListFacade);
			}
			else
			{
				masterListFacade.updateContentsNoOrder(workingListFacade.getContents());
			}
			updateAbilityCategoryTodo((AbilityCategory)category);
		}
		
		Set<AbilityCategoryFacade> origCats = new HashSet<AbilityCategoryFacade>(abilityListMap.keySet());
		for (AbilityCategoryFacade category : origCats)
		{
			if (!workingAbilityListMap.containsKey(category))
			{
				if (workingActiveCategories.containsElement(category))
				{
					abilityListMap.get(category).clearContents();
				}
				else
				{
					abilityListMap.remove(category);
				}
				updateAbilityCategoryTodo((AbilityCategory)category);
			}
		}
		activeCategories.updateContents(workingActiveCategories.getContents());
	}


	private void updateAbilityCategoryTodo(Category<Ability> cat)
	{
		if (!(cat instanceof AbilityCategory))
		{
			return;
		}
		AbilityCategory category = (AbilityCategory) cat;
		
		int numSelections = theCharacter.getAvailableAbilityPool(category).intValue();
		if (category.getVisibility().isVisibleTo(View.VISIBLE_EXPORT))
		{
			// Hide todos for categories that should not be displayed
			numSelections = 0;
		}				

		if (numSelections < 0)
		{
			todoManager.addTodo(new TodoFacadeImpl(
				Tab.ABILITIES, category.getDisplayName(),
				"in_featTodoTooMany", category.getType(), 1)); //$NON-NLS-1$
			todoManager.removeTodo("in_featTodoRemain", category.getDisplayName()); //$NON-NLS-1$
		}
		else if (numSelections > 0)
		{
			todoManager.addTodo(new TodoFacadeImpl(
				Tab.ABILITIES, category.getDisplayName(),
				"in_featTodoRemain", category.getType(), 1)); //$NON-NLS-1$
			todoManager.removeTodo("in_featTodoTooMany", category.getDisplayName()); //$NON-NLS-1$
		}
		else
		{
			todoManager.removeTodo("in_featTodoRemain", category.getDisplayName()); //$NON-NLS-1$
			todoManager.removeTodo("in_featTodoTooMany", category.getDisplayName()); //$NON-NLS-1$
		}
	}
	

	/**
	 * Determine where the ability category should be added to the active 
	 * category list. This will keep the activate categories in the same sort 
	 * order as the activity category list. 
	 * @param abilityCategory The category being added 
	 * @return The index at which to insert the category.
	 */
	private int getCatIndex(AbilityCategory abilityCategory, ListFacade<AbilityCategoryFacade> catList)
	{
		ListFacade<AbilityCategoryFacade> allCategories = dataSetFacade.getAbilityCategories();
		int index = 0;
		for (int i = 0; i < allCategories.getSize(); i++)
		{
			AbilityCategoryFacade compCat = allCategories.getElementAt(i);
			if (compCat == abilityCategory || index >= catList.getSize())
			{
				break;
			}
			if (catList.getElementAt(index) == compCat)
			{
				index++;
			}
		}
		return index;
	}

	/**
	 * Add the ability to the categorised list held by CharacterAbilities. 
	 * One copy will be added for each choice.
	 * 
	 * @param cat The AbilityCategory that the ability is being added to.
	 * @param ability The ability being added.
	 * @param nature The nature via which the ability is being added.
	 * @param workingAbilityListMap The map to be adjusted.
	 */
	private void addCategorisedAbility(AbilityCategory cat,
		Ability ability, Nature nature, Map<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>> workingAbilityListMap)
	{
		List<CNAbilitySelection> cas = new ArrayList<CNAbilitySelection>();
		if (ability.getSafe(ObjectKey.MULTIPLE_ALLOWED))
		{
			List<String> choices = theCharacter.getAssociationList(ability);
			if (choices == null || choices.isEmpty())
			{
				//TODO shouldn't this be a warning - the core should not let this happen?
				cas.add(new CNAbilitySelection(new CNAbility(cat, ability, nature),
					""));
			}
			else
			{
				for (String choice : choices)
				{
					cas.add(new CNAbilitySelection(new CNAbility(cat, ability,
						nature), choice));
				}
			}
			
		}
		else
		{
			cas.add(new CNAbilitySelection(new CNAbility(cat, ability, nature)));
		}
		for (CNAbilitySelection sel : cas)
		{
			addElement(workingAbilityListMap, sel);
		}
	}

	private void removeCategorisedAbility(AbilityCategory cat,
		Ability ability, Nature nature)
	{
		CNAbilitySelection cas;
		if (ability.getSafe(ObjectKey.MULTIPLE_ALLOWED))
		{
			cas = new CNAbilitySelection(new CNAbility(cat, ability, nature), "");
		}
		else
		{
			cas = new CNAbilitySelection(new CNAbility(cat, ability, nature));
		}
		removeElement(cas);
	}
	
	/**
	 * Process a request by the user to add an ability. The user will be informed 
	 * if the request cannot be allowed. Updates to the displayed lists are 
	 * handled by events (see initForCharacter).
	 * 
	 * @param categoryFacade The category in which the ability s bing added.
	 * @param abilityFacade The ability to be added.
	 */
	public void addAbility(AbilityCategoryFacade categoryFacade,
		AbilityFacade abilityFacade)
	{
		if (abilityFacade == null || !(abilityFacade instanceof Ability)
			|| categoryFacade == null
			|| !(categoryFacade instanceof AbilityCategory))
		{
			return;
		}

		Ability ability = (Ability) abilityFacade;
		AbilityCategory category = (AbilityCategory) categoryFacade;
		if (!checkAbilityQualify(ability, category))
		{
			return;
		}


		// we can only be here if the PC can add the ability
		try
		{
			theCharacter.setDirty(true);

			theCharacter.getSpellList();

			Ability pcAbility =
					theCharacter.addAbilityNeedCheck(category, ability);
				
			AbilityUtilities.finaliseAbility(pcAbility, Constants.EMPTY_STRING,
					theCharacter, category);
		}
		catch (Exception exc)
		{
			Logging.errorPrint("Failed to add ability due to ", exc);
			ShowMessageDelegate.showMessageDialog(LanguageBundle
				.getFormattedString("in_iayAddAbility", exc.getMessage()), //$NON-NLS-1$
				Constants.APPLICATION_NAME, MessageType.ERROR);
		}


		// Recalc the innate spell list
		theCharacter.getSpellList();

		theCharacter.calcActiveBonuses();

		// update the ability info
		rebuildAbilityLists();
	}

	/**
	 * Process a request by the user to remove an ability. The user will be  
	 * informed if the request cannot be allowed. Updates to the displayed 
	 * lists are handled by events (see initForCharacter).
	 * 
	 * @param categoryFacade The category from which the ability is being removed.
	 * @param abilityFacade The ability to be removed.
	 */
	public void removeAbility(AbilityCategoryFacade categoryFacade,
		AbilityFacade abilityFacade)
	{
		if (abilityFacade == null || !(abilityFacade instanceof Ability)
			|| categoryFacade == null
			|| !(categoryFacade instanceof AbilityCategory))
		{
			return;
		}

		Ability anAbility = (Ability) abilityFacade;
		AbilityCategory theCategory = (AbilityCategory) categoryFacade;

		try
		{
			theCharacter.setDirty(true);
			if (!theCharacter.isImporting())
			{
				theCharacter.getSpellList();
			}

			Ability pcAbility =
					theCharacter.getMatchingAbility(theCategory, anAbility,
						Nature.NORMAL);

			if (pcAbility != null)
			{
				// how many sub-choices to make
				double abilityCount =
						(theCharacter
							.getSelectCorrectedAssociationCount(pcAbility) * pcAbility
							.getSafe(ObjectKey.SELECTION_COST).doubleValue());

				boolean adjustedAbilityPool = false;

				// adjust the associated List
				if (pcAbility.getSafe(ObjectKey.MULTIPLE_ALLOWED))
				{
					// Get modChoices to adjust the associated list and Feat Pool
					adjustedAbilityPool =
							ChooserUtilities.modChoices(pcAbility,
								new ArrayList<String>(),
								new ArrayList<String>(), theCharacter, false,
								theCategory);
				}

				// if no sub choices made (i.e. all of them removed in Chooser box),
				// then remove the Feat
				boolean removed = false;
				boolean result =
						pcAbility.getSafe(ObjectKey.MULTIPLE_ALLOWED)
							? theCharacter.hasAssociations(pcAbility) : false;

				if (!result)
				{
					removed =
							theCharacter.removeRealAbility(theCategory,
								pcAbility);
					CDOMObjectUtilities.removeAdds(pcAbility, theCharacter);
					CDOMObjectUtilities
						.restoreRemovals(pcAbility, theCharacter);
				}

				if (!adjustedAbilityPool
					&& (theCategory == AbilityCategory.FEAT))
				{
					AbilityUtilities.adjustPool(pcAbility, theCharacter, false,
						abilityCount, removed);
				}

				theCharacter.adjustMoveRates();
			}
		}
		catch (Exception exc)
		{
			Logging.errorPrintLocalised("in_iayFailedToRemoveAbility", exc); //$NON-NLS-1$
			delegate.showErrorMessage(Constants.APPLICATION_NAME, LanguageBundle
				.getString("in_iayRemoveAbility") //$NON-NLS-1$
				+ ": " + exc.getMessage());
			return;
		}

		theCharacter.calcActiveBonuses();

		// update the ability info
		rebuildAbilityLists();
		return;
	}

	/**
	 * Retrieve the list of abilities for this category. The list
	 * will be updated when abilities are added and removed.
	 *  
	 * @param category The ability category to be retrieved.
	 * @return The list of abilities.
	 */
	public ListFacade<AbilityFacade> getAbilities(AbilityCategoryFacade category)
	{
		DefaultListFacade<AbilityFacade> abList = abilityListMap.get(category);
		if (abList == null)
		{
			abList = new DefaultListFacade<AbilityFacade>();
			abilityListMap.put(category, abList);
		}
		return abList;
	}
	
	/**
	 * @return The list of active ability categories.
	 */
	public ListFacade<AbilityCategoryFacade> getActiveAbilityCategories()
	{
		return activeCategories;
	}

	/**
	 * Get the total number of selections for this category.
	 * @param categoryFacade The ability category to be retrieved.
	 * @return The total number of choices.
	 */
	public int getTotalSelections(AbilityCategoryFacade categoryFacade)
	{
		if (categoryFacade == null
			|| !(categoryFacade instanceof AbilityCategory))
		{
			return 0;
		}

		AbilityCategory category = (AbilityCategory) categoryFacade;
		BigDecimal pool = theCharacter.getTotalAbilityPool(category);
		return pool.intValue();
	}

	/**
	 * Get the number of selections that are remaining for this category.
	 * @param categoryFacade The ability category to be retrieved.
	 * @return The number of choices left.
	 */
	public int getRemainingSelections(AbilityCategoryFacade categoryFacade)
	{
		if (categoryFacade == null
			|| !(categoryFacade instanceof AbilityCategory))
		{
			return 0;
		}

		AbilityCategory category = (AbilityCategory) categoryFacade;
		BigDecimal pool = theCharacter.getAvailableAbilityPool(category);
		return pool.intValue();
	}

	/**
	 * Set the number of selections that are remaining for this category.
	 * @param categoryFacade The ability category to be set.
	 * @param remaining The number of choices left.
	 */
	public void setRemainingSelection(AbilityCategoryFacade categoryFacade,
		int remaining)
	{
		if (categoryFacade == null
			|| !(categoryFacade instanceof AbilityCategory))
		{
			return;
		}

		AbilityCategory category = (AbilityCategory) categoryFacade;
		BigDecimal pool = theCharacter.getAvailableAbilityPool(category);

		final BigDecimal newRemain = new BigDecimal(remaining);
		if (pool.equals(newRemain))
		{
			return;
		}
		
		theCharacter.adjustAbilities(category, newRemain
			.subtract(pool));
	}

	/**
	 * Check if the character has an ability.
	 * @param category The ability category to be checked. 
	 * @param ability The ability to be checked.
	 * @return true if the character has the ability, false otherwise.
	 */
	public boolean hasAbility(AbilityCategoryFacade category,
		AbilityFacade ability)
	{
		DefaultListFacade<AbilityFacade> abList = abilityListMap.get(category);
		if (abList == null)
		{
			return false;
		}
		return abList.containsElement(ability);

	}

	/**
	 * Register a listener to be advised of potential changes in the number of 
	 * selections for an ability category. 
	 * @param listener The class to be advised of a change.
	 */
	public void addAbilityCatSelectionListener(ChangeListener listener)
	{
		abilityCatSelectionListeners.add(listener);
	}

	/**
	 * Deregister a listener that should no longer be advised of potential changes
	 * in the number of selections for an ability category. 
	 * @param listener The class to no longer be advised of a change.
	 */
	public void removeAbilityCatSelectionListener(ChangeListener listener)
	{
		abilityCatSelectionListeners.remove(listener);
	}
	
	/**
	 * Advise any listeners that the number of selections may have changed. 
	 * @param cat The ability category that may have changed.
	 */
	private void fireAbilityCatSelectionUpdated(AbilityCategory cat)
	{
		ChangeEvent event = null;
		for (ChangeListener listener : abilityCatSelectionListeners)
		{
			if (event == null)
			{
				event = new ChangeEvent(cat);
			}
			listener.ItemChanged(event);
		}
	}
	
	/**
	 * After any other processing has finished, advise any listeners that 
	 * the number of selections may have changed. 
	 * @param cat The ability category that may have changed.
	 */
	private void adviseSelectionChangeLater(final AbilityCategory cat)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				updateAbilityCategoryTodo(cat);
				fireAbilityCatSelectionUpdated(cat);
				refreshChoices(cat);
			}
		});
	}
	
	/**
	 * After any other processing has finished, refresh the todo information. 
	 * This occurs as category totals are updated after we are notified of the 
	 * abilities being added or removed.
	 * @param category The ability category that may have changed.
	 */
	private void updateAbilityCategoryLater(final Category<Ability> category)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				updateAbilityCategoryTodo(category);
			}
		});
	}
	
	/**
	 * Signal that any ability that could have choices has been modified. This 
	 * ensures that the choice display is up to date.
	 * @param category The ability category being refreshed.
	 */
	protected void refreshChoices(Category<Ability> category)
	{
		DefaultListFacade<AbilityFacade> listFacade = abilityListMap.get(category);
		if (listFacade == null)
		{
			return;
		}
		for (AbilityFacade abilityFacade : listFacade)
		{
			Ability ability = (Ability) abilityFacade;
			if (ability.getSafe(ObjectKey.MULTIPLE_ALLOWED))
			{
				listFacade.modifyElement(ability);
			}
		}
	}

	private boolean checkAbilityQualify(final Ability anAbility,
		AbilityCategory theCategory)
	{
		final String aKey = anAbility.getKeyName();
		boolean pcHasIt = theCharacter.hasAbilityKeyed(theCategory, aKey);

		if (pcHasIt && !anAbility.getSafe(ObjectKey.MULTIPLE_ALLOWED))
		{
			delegate.showErrorMessage(Constants.APPLICATION_NAME, LanguageBundle
				.getString("InfoAbility.Messages.Duplicate")); //$NON-NLS-1$
			return false;
		}

		//TODO Why do we regrab the context-based Ability when an Ability was passed in?
		Ability ability =
				Globals.getContext().ref.silentlyGetConstructedCDOMObject(
					Ability.class, theCategory, aKey);
		if (ability != null
			&& !ability.qualifies(theCharacter, ability)
			&& (!Globals.checkRule(RuleConstants.FEATPRE) || !AbilityUtilities
				.isFeat(ability)))
		{
			delegate.showErrorMessage(Constants.APPLICATION_NAME, LanguageBundle
				.getString("InfoAbility.Messages.NotQualified")); //$NON-NLS-1$
			return false;
		}

		if ((ability != null))
		{
			final BigDecimal cost = ability.getSafe(ObjectKey.SELECTION_COST);
			if (cost.compareTo(theCharacter
				.getAvailableAbilityPool(theCategory)) > 0)
			{
				delegate.showErrorMessage(Constants.APPLICATION_NAME, LanguageBundle
					.getString("InfoAbility.Messages.NoPoints")); //$NON-NLS-1$
				return false;
			}
		}

		return true;
	}

	private void addElement(Map<AbilityCategoryFacade, DefaultListFacade<AbilityFacade>> workingAbilityListMap, CNAbilitySelection cnas)
	{
		CNAbility cas = cnas.getCNAbility();
		Ability ability = cas.getAbility();
		if (!ability.getSafe(ObjectKey.VISIBILITY).isVisibleTo(View.VISIBLE_DISPLAY))
		{
			// Filter out hidden abilities
			return;
		}
		AbilityCategoryFacade cat = (AbilityCategoryFacade) cas.getAbilityCategory();
		DefaultListFacade<AbilityFacade> listFacade = workingAbilityListMap.get(cat);
		if (listFacade == null)
		{
			listFacade = new  DefaultListFacade<AbilityFacade>();
			workingAbilityListMap.put(cat, listFacade);
		}
		if (!listFacade.containsElement(ability))
		{
			listFacade.addElement(ability);
		}
	}

	private void removeElement(CNAbilitySelection cnas)
	{
		CNAbility cas = cnas.getCNAbility();
		Ability ability = cas.getAbility();
		AbilityCategoryFacade cat = (AbilityCategoryFacade) cas.getAbilityCategory();
		DefaultListFacade<AbilityFacade> listFacade = abilityListMap.get(cat);
		if (listFacade != null)
		{
			listFacade.removeElement(ability);
		}
	}

	
	/**
	 * The Class <code>DirectAbilityChangeHandler</code> responds to changes to 
	 * the character's list of direct abilities.
	 */
	private final class DirectAbilityChangeHandler implements
			DataFacetChangeListener<CharID, CNAbilitySelection>
	{
		@SuppressWarnings("nls")
		@Override
		public void dataAdded(
			DataFacetChangeEvent<CharID, CNAbilitySelection> dfce)
		{
			if (dfce.getCharID() != charID)
			{
				// The change notification is not for this character, so ignore it.
//				Logging.debugPrint("CA for " + theCharacter.getName() 
//					+ ". Ignoring direct ability added for character " 
//					+ dfce.getCharID());
				return;
			}
			CNAbilitySelection cnas = dfce.getCDOMObject();
			CNAbility cas = cnas.getCNAbility();
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got direct ability added of "
					+ cas.getAbilityKey() + " for cat "
					+ cas.getAbilityCategory());
			}
			addElement(abilityListMap, cnas);
			updateAbilityCategoryLater(cas.getAbilityCategory());
		}

		@SuppressWarnings("nls")
		@Override
		public void dataRemoved(
			DataFacetChangeEvent<CharID, CNAbilitySelection> dfce)
		{
			if (dfce.getCharID() != charID)
			{
				// The change notification is not for this character, so ignore it.
				return;
			}
			CNAbilitySelection cnas = dfce.getCDOMObject();
			CNAbility cas = cnas.getCNAbility();
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got direct ability removed of "
					+ cas.getAbilityKey() + " for cat "
					+ cas.getAbilityCategory());
			}
			removeElement(cnas);
			updateAbilityCategoryLater(cas.getAbilityCategory());
		}
	}

	/**
	 * The Class <code>GrantedAbilityChangeHandler</code> responds to changes to 
	 * the character's list of granted abilities.
	 */
	private final class GrantedAbilityChangeHandler implements
			DataFacetChangeListener<CharID, CNAbilitySelection>
	{
		@SuppressWarnings("nls")
		@Override
		public void dataAdded(DataFacetChangeEvent<CharID, CNAbilitySelection> dfce)
		{
			if (dfce.getCharID() != charID)
			{
//					Logging.debugPrint("CA for " + theCharacter.getName()
//						+ ". Ignoring granted ability added for character "
//						+ dfce.getCharID());
				return;
			}
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got granted ability added of "
					+ dfce.getCDOMObject());
			}
			//Ability ability = dfce.getCDOMObject();
			rebuildAbilityLists();
		}

		@SuppressWarnings("nls")
		@Override
		public void dataRemoved(DataFacetChangeEvent<CharID, CNAbilitySelection> dfce)
		{
			if (dfce.getCharID() != charID)
			{
					Logging
						.debugPrint("CA for "
							+ charDisplay.getName()
							+ ". Ignoring granted ability removed for character "
							+ dfce.getCharID());
				return;
			}
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got granted ability removed of "
					+ dfce.getCDOMObject());
			}
			//Ability ability = dfce.getCDOMObject();
			rebuildAbilityLists();
		}
	}

	/**
	 * The Class <code>ActiveAbilityChangeHandler</code> responds to changes to 
	 * the character's list of active abilities.
	 */
	private final class ActiveAbilityChangeHandler implements
			DataFacetChangeListener<CharID, Ability>
	{
		@SuppressWarnings("nls")
		@Override
		public void dataAdded(DataFacetChangeEvent<CharID, Ability> dfce)
		{
			if (dfce.getCharID() != charID)
			{
//					Logging.debugPrint("CA for " + theCharacter.getName()
//						+ ". Ignoring active ability added for character "
//						+ dfce.getCharID());
				return;
			}
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got active ability added of "
					+ dfce.getCDOMObject());
			}
			Ability ability = dfce.getCDOMObject();
			if (dfce instanceof CategorizedDataFacetChangeEvent)
			{
				
				CategorizedDataFacetChangeEvent<CharID, ?> categorizedEvent =
						(CategorizedDataFacetChangeEvent<CharID, ?>) dfce;
				AbilityCategory cat = (AbilityCategory) categorizedEvent.getCategory();
				Nature nature = categorizedEvent.getNature();
				addAbilityToLists(cat, ability, nature);
			}
		}

		@SuppressWarnings("nls")
		@Override
		public void dataRemoved(DataFacetChangeEvent<CharID, Ability> dfce)
		{
			if (dfce.getCharID() != charID)
			{
					Logging
						.debugPrint("CA for "
							+ charDisplay.getName()
							+ ". Ignoring active ability removed for character "
							+ dfce.getCharID());
				return;
			}
			if (Logging.isDebugMode())
			{
				Logging.debugPrint("Got active ability removed of "
					+ dfce.getCDOMObject());
			}
			Ability ability = dfce.getCDOMObject();
			if (dfce instanceof CategorizedDataFacetChangeEvent)
			{
				
				CategorizedDataFacetChangeEvent<CharID, ?> categorizedEvent =
						(CategorizedDataFacetChangeEvent<CharID, ?>) dfce;
				AbilityCategory cat = (AbilityCategory) categorizedEvent.getCategory();
				Nature nature = categorizedEvent.getNature();
				removeAbilityFromLists(cat, ability, nature);
			}
		}
	}

}
