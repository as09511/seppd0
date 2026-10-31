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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.malkuth;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.sprites.LCorpEmployeeSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//an L Corp employee called in by the Control Team's sephirah, fights alongside her until it falls
public class LCorpEmployee extends DirectableAlly {

	{
		spriteClass = LCorpEmployeeSprite.class;

		HP = HT = 10;

		immunities.add(AllyBuff.class);
	}

	//the hero's level when the employee was called, sets its strength
	private int heroLevel = 1;

	public LCorpEmployee(){
		super();
	}

	public LCorpEmployee( Hero hero ){
		super();
		heroLevel = hero.lvl;
		//half of the hero's max health
		HP = HT = Math.max(1, hero.HT/2);
		defenseSkill = heroLevel + 4; //equal to base hero defense skill
	}

	@Override
	protected boolean act() {
		int oldPos = pos;
		boolean result = super.act();
		//partially simulates how the hero switches to idle animation
		if ((pos == target || oldPos == pos) && sprite != null && sprite.looping()){
			sprite.idle();
		}
		return result;
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill + 5; //equal to base hero attack skill
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2, 5 + heroLevel);
	}

	@Override
	public float speed() {
		float speed = super.speed();
		//catches up quickly when returning to the hero
		if (state == WANDERING && defendingPos == -1
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) > 1){
			speed *= 2;
		}
		return speed;
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		EmployeeCall call = Dungeon.hero == null ? null : Dungeon.hero.buff(EmployeeCall.class);
		if (call != null){
			call.onEmployeeDied();
		}
	}

	private static final String HERO_LEVEL = "hero_level";
	private static final String DEF_SKILL = "def_skill";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HERO_LEVEL, heroLevel);
		bundle.put(DEF_SKILL, defenseSkill);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		heroLevel = bundle.getInt(HERO_LEVEL);
		defenseSkill = bundle.getInt(DEF_SKILL);
	}
}
