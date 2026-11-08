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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.tiphereth.TipherethTrackers;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.hod.HodTrackers;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.netzach.NetzachTrackers;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Analyzed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.YesodTrackers;
import com.shatteredpixel.shatteredpixeldungeon.items.ConcentratedEnkephalin;

//The sephirot's "damage dealt" bonuses, kept in one place so every kind of damage the hero deals gets them once:
//attacks (melee, shots, thrown weapons, abilities that strike) go through Char.attack, while weapon and ability
//damage dealt without an attack roll (splash, extra hits, beams, blasts) uses direct(). Allies never get them.
public class HeroDamageBonus {

	//All of these apply after armor. The flat bonus is added first, then the percent bonuses of every
	// source are added together into a single multiplier.

	//flat bonus: Netzach's hangover, +2/+3 while taking delayed damage
	public static int flatBonus( Hero hero ){
		if (hero.hasTalent(Talent.HANGOVER) && hero.buff(Viscosity.DeferedDamage.class) != null){
			return 1 + hero.pointsInTalent(Talent.HANGOVER);
		}
		return 0;
	}

	//the percent bonuses, summed: 0.25 means +25%
	public static float percentBonus( Hero hero, Char target ){
		float bonus = 0f;
		//Netzach's overdose: +15/25% for a while after concentrated enkephalin
		bonus += ConcentratedEnkephalin.Overdose.damageFactor(hero) - 1f;
		//Hod's training result and the literature floor librarian's bonus on rooted enemies
		if (hero.heroClass == HeroClass.HOD){
			bonus += HodTrackers.attackMultiplier(hero, target) - 1f;
		}
		//Netzach's finale: more damage against enemies with two or more debuffs
		if (hero.heroClass == HeroClass.NETZACH){
			bonus += NetzachTrackers.attackMultiplier(hero, target) - 1f;
		}
		//Tiphereth's emptiness: more damage for each empty artifact or ring slot
		if (hero.heroClass == HeroClass.TIPHERETH){
			bonus += TipherethTrackers.emptinessDealt(hero) - 1f;
		}
		//Yesod's viper Yesod, charge and black flame
		if (hero.heroClass == HeroClass.YESOD){
			bonus += YesodTrackers.attackMultiplier(hero) - 1f;
		}
		return bonus;
	}

	//applied to the damage left once armor is subtracted
	public static int afterArmor( Hero hero, Char target, int dmg ){
		int flat = flatBonus(hero);
		float bonus = percentBonus(hero, target);
		if (flat == 0 && bonus == 0f) return dmg;
		return Math.max(0, Math.round((dmg + flat) * (1f + bonus)));
	}

	//damage dealt to a target without an attack roll, with the given armor roll subtracted (0 for none)
	public static int direct( Char attacker, Char target, int dmg, int armor ){
		if (!(attacker instanceof Hero) || dmg <= 0) return Math.max(0, dmg - armor);
		Hero hero = (Hero) attacker;
		float boosted = dmg;
		//G.O visualization +2: analyzed enemies take 20% more, the same as from an attack
		if (target != null && target.buff(Analyzed.class) != null && hero.pointsInTalent(Talent.GO_VISUALIZATION) >= 2){
			boosted *= 1.2f;
		}
		int result = Math.max(0, Math.round(boosted) - armor);
		return afterArmor(hero, target, result);
	}

	public static int direct( Char attacker, Char target, int dmg ){
		return direct(attacker, target, dmg, 0);
	}
}
