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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.hod;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.hod.ManagerCall;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.sprites.LCorpEmployeeSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//an L Corp manager called in by Hod's manager call: twice an L Corp employee's health and damage, with armor
public class LCorpManager extends DirectableAlly {

	{
		spriteClass = LCorpEmployeeSprite.class;

		HP = HT = 20;

		immunities.add(AllyBuff.class);
	}

	//the hero's level when the manager was called, and the hiring procedure multiplier
	private int heroLevel = 1;
	private float mult = 1f;

	public LCorpManager(){
		super();
	}

	public LCorpManager( Hero hero ){
		super();
		heroLevel = hero.lvl;
		//hiring procedure: +25% health, damage and armor per point
		mult = 1f + 0.25f*hero.pointsInTalent(Talent.HIRING_PROCEDURE);
		//twice an employee's health: the hero's max health
		HP = HT = Math.max(1, Math.round(hero.HT * mult));
		defenseSkill = heroLevel + 4; //equal to base hero defense skill, like an employee
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

	//L Corp's staff deal RED damage
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, DamageType.RED, damage);
		return super.attackProc(enemy, damage);
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill + 5; //equal to base hero attack skill
	}

	//twice an employee's damage
	@Override
	public int damageRoll() {
		return Random.NormalIntRange(Math.round(4*mult), Math.round((10 + 2*heroLevel)*mult));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Math.round(10*mult));
	}

	@Override
	public void damage(int dmg, Object src) {
		//field guidance: less damage while next to Hod
		dmg = Math.round(dmg * ManagerCall.guidanceFactor(Dungeon.hero, this));
		super.damage(dmg, src);
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

	private static final String HERO_LEVEL = "hero_level";
	private static final String DEF_SKILL = "def_skill";
	private static final String MULT = "mult";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HERO_LEVEL, heroLevel);
		bundle.put(DEF_SKILL, defenseSkill);
		bundle.put(MULT, mult);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		heroLevel = bundle.getInt(HERO_LEVEL);
		defenseSkill = bundle.getInt(DEF_SKILL);
		mult = bundle.getFloat(MULT);
	}
}
