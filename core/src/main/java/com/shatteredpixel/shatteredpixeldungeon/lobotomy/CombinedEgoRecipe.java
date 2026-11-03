/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;

import java.util.ArrayList;

//the alchemy recipe of a combined E.G.O: the result takes the highest upgrade level among the ingredients
//and comes identified, and the preview in the alchemy pot shows it the same way
public abstract class CombinedEgoRecipe extends Recipe.SimpleRecipe {

	private int upgradeLevel( ArrayList<Item> ingredients ){
		int level = 0;
		if (ingredients == null) return level;
		for (Item i : ingredients){
			for (Class<? extends Item> input : inputs){
				if (i.getClass() == input){
					level = Math.max(level, i.level());
				}
			}
		}
		return level;
	}

	private Item finish( Item result, int level ){
		if (result != null){
			if (level > 0) result.upgrade(level);
			result.identify(false);
		}
		return result;
	}

	@Override
	public Item brew( ArrayList<Item> ingredients ) {
		int level = upgradeLevel(ingredients);
		Item result = finish(super.brew(ingredients), level);
		//the real thing is known to the catalog, unlike the preview
		if (result != null) result.identify();
		return result;
	}

	@Override
	public Item sampleOutput( ArrayList<Item> ingredients ) {
		return finish(super.sampleOutput(ingredients), upgradeLevel(ingredients));
	}
}
