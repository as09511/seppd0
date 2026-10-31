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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

public class Haste extends FlavourBuff {
	
	{
		type = buffType.POSITIVE;
	}
	
	public static final float DURATION	= 20f;
	
	@Override
	public int icon() {
		return BuffIndicator.HASTE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.8f, 0f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	//gaining haste from any source, see Buff.affect and Buff.prolong
	@Override
	protected void spend(float time) {
		super.spend(time);
		onGained(time);
	}

	@Override
	protected void postpone(float time) {
		super.postpone(time);
		onGained(time);
	}

	private boolean extending = false;

	//adds time without counting as gaining haste again, for the speed war talent
	public void extend(float time){
		extending = true;
		spend(time);
		extending = false;
	}

	private void onGained(float time){
		//safety protocol: 3/5 shielding whenever the hero gains haste
		if (!extending && time > 0 && target instanceof Hero && ((Hero) target).hasTalent(Talent.SAFETY_PROTOCOL)){
			Buff.affect(target, Barrier.class).setShield(1 + 2*((Hero) target).pointsInTalent(Talent.SAFETY_PROTOCOL));
		}
	}

}
