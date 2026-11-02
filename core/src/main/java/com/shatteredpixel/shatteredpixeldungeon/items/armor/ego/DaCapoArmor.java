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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.DaCapo;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//ALEPH, same defense as the tier 5 armor
//worn together with the Da Capo weapon, WHITE damage heals the wearer instead (multiplier -1.0)
public class DaCapoArmor extends EgoArmor {

	public static final float ENSEMBLE_WHITE = -1f;

	{
		image = ItemSpriteSheet.DA_CAPO_ARMOR;

		risk = RiskLevel.ALEPH;
		damageMultipliers = new float[]{0.5f, 0.2f, 0.5f, 1.5f};
	}

	public DaCapoArmor() {
		super( 5 );
	}

	//whether this armor is worn by the hero along with the Da Capo weapon
	private boolean ensemble(){
		Hero hero = Dungeon.hero;
		return hero != null && hero.belongings.armor() == this
				&& (hero.belongings.weapon() instanceof DaCapo || hero.belongings.secondWep() instanceof DaCapo);
	}

	@Override
	public float damageMultiplier(DamageType type) {
		if (type == DamageType.WHITE && ensemble()){
			return ENSEMBLE_WHITE;
		}
		return super.damageMultiplier(type);
	}

	@Override
	protected String statsInfo() {
		return super.statsInfo() + "\n\n" + Messages.get(this, "stats_desc",
				Messages.decimalFormat("0.0#", ENSEMBLE_WHITE));
	}

}
