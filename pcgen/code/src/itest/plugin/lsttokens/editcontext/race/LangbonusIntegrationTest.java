/*
 * Copyright (c) 2007 Tom Parker <thpr@users.sourceforge.net>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package plugin.lsttokens.editcontext.race;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import pcgen.cdom.list.LanguageList;
import pcgen.core.Language;
import pcgen.core.Race;
import pcgen.persistence.PersistenceLayerException;
import pcgen.rules.persistence.CDOMLoader;
import pcgen.rules.persistence.token.CDOMPrimaryToken;
import plugin.lsttokens.editcontext.testsupport.AbstractListIntegrationTestCase;
import plugin.lsttokens.race.LangbonusToken;
import plugin.lsttokens.testsupport.CDOMTokenLoader;

public class LangbonusIntegrationTest extends
		AbstractListIntegrationTestCase<Race, Language>
{

	static LangbonusToken token = new LangbonusToken();
	static CDOMTokenLoader<Race> loader = new CDOMTokenLoader<Race>(Race.class);

	@Override
	@Before
	public void setUp() throws PersistenceLayerException, URISyntaxException
	{
		super.setUp();
		/*
		 * FIXME This construction of *Starting should be unnecessary
		 */
		primaryContext.ref.constructCDOMObject(LanguageList.class, "*Starting");
		secondaryContext.ref.constructCDOMObject(LanguageList.class,
				"*Starting");
	}

	@Override
	public Class<Race> getCDOMClass()
	{
		return Race.class;
	}

	@Override
	public CDOMLoader<Race> getLoader()
	{
		return loader;
	}

	@Override
	public CDOMPrimaryToken<Race> getToken()
	{
		return token;
	}

	@Override
	public Class<Language> getTargetClass()
	{
		return Language.class;
	}

	@Override
	public boolean isTypeLegal()
	{
		return true;
	}

	@Override
	public char getJoinCharacter()
	{
		return ',';
	}

	@Test
	public void dummyTest()
	{
		// Just to get Eclipse to recognize this as a JUnit 4.0 Test Case
	}

	@Override
	public boolean isClearDotLegal()
	{
		return true;
	}

	@Override
	public boolean isClearLegal()
	{
		return true;
	}

	@Override
	public boolean isPrereqLegal()
	{
		return false;
	}

	@Override
	public boolean isAllLegal()
	{
		return true;
	}
}
