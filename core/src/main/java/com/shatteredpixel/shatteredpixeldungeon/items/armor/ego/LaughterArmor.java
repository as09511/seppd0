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


package com.shatteredpixel.shatteredpixeldungeon.items.armor.ego;

import com.shatteredpixel.shatteredpixeldungeon.lobotomy.Corpses;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//ALEPH, same defense as the tier 5 armor
//its wearer can eat the bodies enemies leave behind to heal, see Corpses
public class LaughterArmor extends EgoArmor {

	{
		image = ItemSpriteSheet.LAUGHTER_ARMOR;

		risk = RiskLevel.ALEPH;
		damageMultipliers = new float[]{0.5f, 0.5f, 0.2f, 1f};
	}

	public LaughterArmor() {
		super( 5 );
	}

	@Override
	protected String statsInfo() {
		return super.statsInfo() + "\n\n" + Messages.get(this, "stats_desc", Corpses.HEAL);
	}

}
