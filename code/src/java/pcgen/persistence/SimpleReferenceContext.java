/*
 * Copyright 2007 (C) Tom Parker <thpr@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.persistence;

import java.util.Date;
import java.util.List;
import java.util.Set;

import pcgen.base.util.DoubleKeyMap;
import pcgen.base.util.DoubleKeyMapToInstanceList;
import pcgen.base.util.HashMapToList;
import pcgen.cdom.base.CDOMAddressedSingleRef;
import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.base.CDOMReference;
import pcgen.cdom.base.CDOMSimpleSingleRef;
import pcgen.cdom.base.CategorizedCDOMObject;
import pcgen.core.Ability;
import pcgen.core.GameMode;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.PObject;
import pcgen.core.SettingsHandler;
import pcgen.core.SizeAdjustment;
import pcgen.core.SystemCollections;
import pcgen.util.Logging;
import pcgen.util.PropertyFactory;

public class SimpleReferenceContext
{

	private DoubleKeyMapToInstanceList<Class, String, PObject> duplicates =
			new DoubleKeyMapToInstanceList<Class, String, PObject>();

	private DoubleKeyMap<Class, String, PObject> active =
			new DoubleKeyMap<Class, String, PObject>();

	private HashMapToList<Class, String> deferred =
			new HashMapToList<Class, String>();

	private DoubleKeyMap<Class, String, CDOMSimpleSingleRef> referenced =
			new DoubleKeyMap<Class, String, CDOMSimpleSingleRef>();

	private DoubleKeyMap<CDOMObject, Class, CDOMAddressedSingleRef> addressed =
			new DoubleKeyMap<CDOMObject, Class, CDOMAddressedSingleRef>();

	public SimpleReferenceContext()
	{
		initialize();
	}

	private void initialize()
	{
		GameMode game = SettingsHandler.getGame();
		List<PCStat> statList = game.getUnmodifiableStatList();
		for (PCStat stat : statList)
		{
			active.put(PCStat.class, stat.getAbb(), stat);
		}
		for (int i = 0; i < game.getSizeAdjustmentListSize(); i++)
		{
			SizeAdjustment sa = game.getSizeAdjustmentAtIndex(i);
			active.put(SizeAdjustment.class, sa.getAbbreviation(), sa);
		}
	}

	public <T extends PObject> void registerWithKey(Class<T> cl, T obj,
		String key)
	{
		if (active.containsKey(cl, key))
		{
			duplicates.addToListFor(cl, key, obj);
		}
		else
		{
			active.put(cl, key, obj);
		}
	}

	public <T extends PObject> T silentlyGetConstructedCDOMObject(Class<T> c,
		String val)
	{
		PObject po = active.get(c, val);
		if (po != null)
		{
			if (duplicates.containsListFor(c, val))
			{
				Logging.errorPrint("Reference to Constructed "
					+ c.getSimpleName() + " " + val + " is ambiguous");
			}
			return (T) po;
		}
		return null;
	}

	public <T extends PObject> T getConstructedCDOMObject(Class<T> c, String val)
	{
		T obj = silentlyGetConstructedCDOMObject(c, val);
		if (obj == null)
		{
			Logging.errorPrint("Someone expected " + c.getSimpleName() + " "
				+ val + " to exist.");
			// Thread.dumpStack();
		}
		return obj;
	}

	public <T extends PObject> T constructCDOMObject(Class<T> c, String val)
	{
		if (val.equals(""))
		{
			throw new IllegalArgumentException("Cannot build empty name");
		}
		try
		{
			/*
			 * BUG FIXME The problem is that Ability DOES end up here - there is
			 * no way of knowing the category in advance... :P
			 */
			if (CategorizedCDOMObject.class.isAssignableFrom(c))
			{
				throw new IllegalArgumentException(c.getSimpleName() + " "
					+ val);
			}
			T obj = c.newInstance();
			obj.setName(val);
			registerWithKey(c, obj, val);
			return obj;
		}
		catch (InstantiationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new IllegalArgumentException(c + " " + val);
	}

	public <T extends PObject> void reassociateReference(String value, T obj)
	{
		String oldKey = obj.getKeyName();
		if (oldKey.equals(value))
		{
			Logging.errorPrint("Worthless Key change encountered: "
				+ obj.getDisplayName() + " " + oldKey);
		}
		Class<T> cl = (Class<T>) obj.getClass();
		PObject act = active.get(cl, oldKey);
		if (act == null)
		{
			throw new InternalError("Did not find " + obj + " under " + oldKey);
		}
		if (act.equals(obj))
		{
			List<PObject> list = duplicates.getListFor(cl, oldKey);
			if (list == null)
			{
				// No replacement
				active.remove(cl, oldKey);
			}
			else
			{
				PObject newActive = duplicates.getItemFor(cl, oldKey, 0);
				duplicates.removeFromListFor(cl, oldKey, newActive);
				active.put(cl, oldKey, newActive);
			}
		}
		else
		{
			duplicates.removeFromListFor(cl, oldKey, obj);
		}
		obj.setKeyName(value);
		registerWithKey(cl, obj, value);
	}

	public <T extends PObject> void forgetCDOMObjectKeyed(Class<T> cl,
		String forgetKey)
	{
		active.remove(cl, forgetKey);
		duplicates.removeListFor(cl, forgetKey);
	}

	public <T extends PObject> Set<T> getConstructedCDOMObjects(Class<T> name)
	{
		return (Set<T>) active.values(name);
	}

	public <T extends PObject> boolean containsConstructedCDOMObject(
		Class<T> name, String key)
	{
		return active.containsKey(name, key);
	}

	public <T extends PObject> T cloneConstructedCDOMObject(Class<T> cl, T obj,
		String newKey)
	{
		try
		{
			T clone = cl.cast(obj.clone());
			clone.setName(newKey);
			clone.setKeyName(newKey);
			registerWithKey(cl, clone, newKey);
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			Logging.errorPrint(PropertyFactory.getFormattedString(
				"Errors.LstFileLoader.CopyNotSupported", //$NON-NLS-1$
				obj.getClass().getName(), obj.getKeyName(), newKey));
		}
		return null;
	}

	public <T extends CDOMObject> CDOMSimpleSingleRef<T> getCDOMReference(
		Class<T> c, String val)
	{
		// TODO Auto-generated method stub
		// TODO This is incorrect, but a hack for now :)
		if (val.equals(""))
		{
			throw new IllegalArgumentException(val);
		}
		try
		{
			Integer.parseInt(val);
			throw new IllegalArgumentException(val);
		}
		catch (NumberFormatException nfe)
		{
			// ok
		}
		if (val.startsWith("TYPE"))
		{
			throw new IllegalArgumentException(val);
		}
		if (val.equalsIgnoreCase("ANY"))
		{
			throw new IllegalArgumentException(val);
		}
		if (val.equalsIgnoreCase("ALL"))
		{
			throw new IllegalArgumentException(val);
		}
		if (val.startsWith("PRE"))
		{
			throw new IllegalArgumentException(val);
		}
		if (val.startsWith("CHOOSE"))
		{
			throw new IllegalArgumentException(val);
		}
		if (val.startsWith("TIMES="))
		{
			throw new IllegalArgumentException(val);
		}
		if (c.equals(PCClass.class))
		{
			if (val.startsWith("CLASS"))
			{
				throw new IllegalArgumentException(val);
			}
			else if (val.startsWith("SUB"))
			{
				throw new IllegalArgumentException(val);
			}
			else
			{
				try
				{
					Integer.parseInt(val);
					throw new IllegalArgumentException(val);
				}
				catch (NumberFormatException nfe)
				{
					// Want this!
				}
			}
		}

		CDOMSimpleSingleRef<T> ref = referenced.get(c, val);
		if (ref == null)
		{
			ref = new CDOMSimpleSingleRef<T>(c, val);
			referenced.put(c, val, ref);
		}
		return ref;
	}

	public boolean validate()
	{
		boolean returnGood = true;
		for (Class key1 : duplicates.getKeySet())
		{
			for (String second : duplicates.getSecondaryKeySet(key1))
			{
				if (SettingsHandler.isAllowOverride())
				{
					List<PObject> list = duplicates.getListFor(key1, second);
					PObject good = active.get(key1, second);
					for (int i = 0; i < list.size(); i++)
					{
						PObject dupe = list.get(i);
						// If the new object is more recent than the current
						// one, use the new object
						final Date origDate =
								good.getSourceEntry().getSourceBook().getDate();
						final Date dupeDate =
								dupe.getSourceEntry().getSourceBook().getDate();
						if ((dupeDate != null)
							&& ((origDate == null) || ((dupeDate
								.compareTo(origDate) > 0))))
						{
							duplicates.removeFromListFor(key1, second, good);
							good = dupe;
						}
						else
						{
							duplicates.removeFromListFor(key1, second, dupe);
						}
					}
					if (!good.equals(active.get(key1, second)))
					{
						active.put(key1, second, good);
					}
				}
				else
				{
					Logging.errorPrint("More than one " + key1.getSimpleName()
						+ " with key/name " + second + " was built");
					returnGood = false;
				}
			}
		}
		for (Class key1 : active.getKeySet())
		{
			for (String second : active.getSecondaryKeySet(key1))
			{
				String keyName = active.get(key1, second).getKeyName();
				if (!keyName.equals(second))
				{
					Logging.errorPrint("Magical Key Change: " + second + " to "
						+ keyName);
					returnGood = false;
				}
			}
		}
		return validateConstructed();
	}

	private boolean validateConstructed()
	{
		boolean returnGood = true;
		for (Class cl : referenced.getKeySet())
		{
			for (String s : referenced.getSecondaryKeySet(cl))
			{
				if (!active.containsKey(cl, s)
					&& !deferred.containsInList(cl, s) && !s.startsWith("*"))
				{
					Logging.errorPrint("Unconstructed Reference: "
						+ cl.getSimpleName() + " " + s);
					returnGood = false;
				}
			}
		}
		return returnGood;
	}

	public <T extends CDOMObject> void constructIfNecessary(Class<T> cl,
		String value)
	{
		/*
		 * TODO FIXME Need to ensure that items that are built here are tagged
		 * as manufactured, so that they are not written out to LST files
		 */
		deferred.addToListFor(cl, value);
	}

	public void clear()
	{
		duplicates.clear();
		active.clear();
		deferred.clear();
		referenced.clear();
	}

	public <T extends PObject> ReferenceManufacturer<T> getReferenceManufacturer(
		final Class<T> c)
	{
		if (Ability.class.equals(c))
		{
			throw new IllegalArgumentException(c.getSimpleName()
				+ " is a Categorized Class");
		}
		return new ReferenceManufacturer<T>()
		{
			public CDOMReference<T> getReference(String key)
			{
				return getCDOMReference(c, key);
			}

			public Class<T> getCDOMClass()
			{
				return c;
			}
		};
	}

	public <T extends PObject> CDOMAddressedSingleRef<T> getAddressedReference(
		CDOMObject obj, Class<T> name, String addressName)
	{
		CDOMAddressedSingleRef<T> addr = addressed.get(obj, name);
		if (addr == null)
		{
			addr = new CDOMAddressedSingleRef<T>(obj, name, addressName);
			addressed.put(obj, name, addr);
		}
		return addr;
	}
}
