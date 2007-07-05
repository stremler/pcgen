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
 *
 * Current Ver: $Revision: 2494 $
 * Last Editor: $Author: thpr $
 * Last Edited: $Date: 2007-03-12 23:59:32 -0400 (Mon, 12 Mar 2007) $
 *
 */
package pcgen.persistence.lst;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.helper.PrimitiveChoiceSet;
import pcgen.persistence.LoadContext;
import pcgen.persistence.PersistenceLayerException;

public interface ChooseCompatibilityToken extends LstToken
{
	public PrimitiveChoiceSet<?> parse(LoadContext context, CDOMObject cdo,
		String value) throws PersistenceLayerException;

	public int compatibilityLevel();

	public int compatibilitySubLevel();

	public int compatibilityPriority();
}
