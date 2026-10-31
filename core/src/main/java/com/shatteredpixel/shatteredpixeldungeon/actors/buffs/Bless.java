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

public class Bless extends FlavourBuff {
	
	public static final float DURATION	= 30f;
	
	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.BLESS;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	//gaining bless from any source, see Buff.affect and Buff.prolong
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

	private void onGained(float time){
		//match flame: imbued with fire for 33/66/100% of the bless gained
		if (time > 0 && target instanceof Hero && ((Hero) target).hasTalent(Talent.MATCH_FLAME)){
			float duration = time * ((Hero) target).pointsInTalent(Talent.MATCH_FLAME) / 3f;
			FireImbue imbue = target.buff(FireImbue.class);
			if (imbue == null){
				Buff.affect(target, FireImbue.class).set(duration);
			} else {
				imbue.set(Math.max(duration, imbue.remaining()));
			}
		}
	}

}
