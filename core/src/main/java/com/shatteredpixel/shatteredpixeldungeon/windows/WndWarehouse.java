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


package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.lobotomy.Warehouse;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//the warehouse, opened from the title screen: the resources shared by every game
public class WndWarehouse extends WndTitledMessage {

	public WndWarehouse(){
		super(new ItemSprite(ItemSpriteSheet.ENKEPHALIN_BOX), Messages.get(WndWarehouse.class, "title"), contents());
	}

	public static String contents(){
		return Messages.get(WndWarehouse.class, "enkephalin", Warehouse.enkephalin())
				+ "\n\n" + Messages.get(WndWarehouse.class, "enkephalin_desc");
	}
}
