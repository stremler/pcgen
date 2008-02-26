package pcgen.rules.context;

import java.util.Collection;
import java.util.TreeSet;

import pcgen.base.util.TreeMapToList;
import pcgen.cdom.base.AssociatedPrereqObject;
import pcgen.cdom.base.CDOMList;
import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.base.LSTWriteable;
import pcgen.cdom.enumeration.AssociationKey;
import pcgen.rules.persistence.TokenUtilities;
import pcgen.util.MapToList;

public class ListChanges<T extends CDOMObject> implements
		AssociatedChanges<CDOMReference<T>>
{
	private String tokenName;
	private CDOMObject positive;
	private CDOMObject negative;
	private CDOMReference<? extends CDOMList<T>> list;
	private boolean clear;

	public ListChanges(String token, CDOMObject added, CDOMObject removed,
			CDOMReference<? extends CDOMList<T>> listref,
			boolean globallyCleared)
	{
		tokenName = token;
		positive = added;
		negative = removed;
		list = listref;
		clear = globallyCleared;
	}

	public boolean includesGlobalClear()
	{
		return clear;
	}

	public boolean isEmpty()
	{
		/*
		 * TODO This lies :P
		 */
		return !clear && !hasAddedItems() && !hasRemovedItems();
	}

	public Collection<LSTWriteable> getAdded()
	{
		TreeSet<LSTWriteable> set = new TreeSet<LSTWriteable>(
				TokenUtilities.WRITEABLE_SORTER);
		Collection<CDOMReference<T>> listMods = positive.getListMods(list);
		if (listMods != null)
		{
			for (CDOMReference<T> ref : listMods)
			{
				for (AssociatedPrereqObject assoc : positive
						.getListAssociations(list, ref))
				{
					if (tokenName.equals(assoc
							.getAssociation(AssociationKey.TOKEN)))
					{
						set.add(ref);
					}
				}
			}
		}
		return set;
	}

	public boolean hasAddedItems()
	{
		/*
		 * TODO This lies :P
		 */
		return positive != null && positive.getListMods(list) != null
				&& !positive.getListMods(list).isEmpty();
	}

	public Collection<LSTWriteable> getRemoved()
	{
		TreeSet<LSTWriteable> set = new TreeSet<LSTWriteable>(
				TokenUtilities.WRITEABLE_SORTER);
		if (negative == null)
		{
			return set;
		}
		Collection<CDOMReference<T>> listMods = negative.getListMods(list);
		if (listMods != null)
		{
			for (CDOMReference<T> ref : listMods)
			{
				for (AssociatedPrereqObject assoc : negative
						.getListAssociations(list, ref))
				{
					if (tokenName.equals(assoc
							.getAssociation(AssociationKey.TOKEN)))
					{
						set.add(ref);
					}
				}
			}
		}
		return set;
	}

	public boolean hasRemovedItems()
	{
		/*
		 * TODO This lies :P
		 */
		return negative != null && negative.getListMods(list) != null
				&& !negative.getListMods(list).isEmpty();
	}

	public MapToList<LSTWriteable, AssociatedPrereqObject> getAddedAssociations()
	{
		MapToList<LSTWriteable, AssociatedPrereqObject> owned = new TreeMapToList<LSTWriteable, AssociatedPrereqObject>(
				TokenUtilities.WRITEABLE_SORTER);
		Collection<CDOMReference<T>> mods = positive.getListMods(list);
		if (mods == null)
		{
			return null;
		}
		for (CDOMReference<T> lw : mods)
		{
			Collection<AssociatedPrereqObject> assocs = positive
					.getListAssociations(list, lw);
			for (AssociatedPrereqObject assoc : assocs)
			{
				if (tokenName
						.equals(assoc.getAssociation(AssociationKey.TOKEN)))
				{
					owned.addToListFor(lw, assoc);
				}
			}
		}
		if (owned.isEmpty())
		{
			return null;
		}
		return owned;
	}

	public MapToList<LSTWriteable, AssociatedPrereqObject> getRemovedAssociations()
	{
		Collection<CDOMReference<T>> mods = negative.getListMods(list);
		MapToList<LSTWriteable, AssociatedPrereqObject> owned = new TreeMapToList<LSTWriteable, AssociatedPrereqObject>(
				TokenUtilities.WRITEABLE_SORTER);
		for (CDOMReference<T> lw : mods)
		{
			Collection<AssociatedPrereqObject> assocs = negative
					.getListAssociations(list, lw);
			for (AssociatedPrereqObject assoc : assocs)
			{
				if (tokenName
						.equals(assoc.getAssociation(AssociationKey.TOKEN)))
				{
					owned.addToListFor(lw, assoc);
				}
			}
		}
		if (owned.isEmpty())
		{
			return null;
		}
		return owned;
	}
}
