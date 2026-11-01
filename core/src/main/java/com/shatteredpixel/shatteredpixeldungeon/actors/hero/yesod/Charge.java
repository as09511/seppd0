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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

//Technological Sciences Floor Librarian: all info poured into a short burst of damage.
// +5% damage per point, losing 1 point every turn
public class Charge extends Buff {

	public static final float DAMAGE_PER_POINT = 0.05f;

	{
		type = buffType.POSITIVE;
	}

	private int charge = 0;

	public void add( int amount ){
		charge += amount;
		BuffIndicator.refreshHero();
	}

	public int charge(){
		return charge;
	}

	public float damageMultiplier(){
		return 1f + DAMAGE_PER_POINT * charge;
	}

	@Override
	public boolean act() {
		charge--;
		if (charge <= 0){
			detach();
		} else {
			BuffIndicator.refreshHero();
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.CHARGE;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(charge);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", charge, Math.round(100 * DAMAGE_PER_POINT * charge));
	}

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}
}
