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

	//bonuses that apply before armor: flat ones first, then the multipliers
	public static float beforeArmor( Hero hero, Char target, float dmg ){
		//Netzach's hangover: +2/+3 while taking delayed damage
		if (hero.hasTalent(Talent.HANGOVER) && hero.buff(Viscosity.DeferedDamage.class) != null){
			dmg += 1 + hero.pointsInTalent(Talent.HANGOVER);
		}
		//Netzach's overdose: +15/25% for a while after concentrated enkephalin
		dmg *= ConcentratedEnkephalin.Overdose.damageFactor(hero);
		//Hod's training result and the literature floor librarian's bonus on rooted enemies
		if (hero.heroClass == HeroClass.HOD){
			dmg *= HodTrackers.attackMultiplier(hero, target);
		}
		//Netzach's finale: more damage against enemies with two or more debuffs
		if (hero.heroClass == HeroClass.NETZACH){
			dmg *= NetzachTrackers.attackMultiplier(hero, target);
		}
		return dmg;
	}

	//bonuses that apply after armor: Yesod's viper Yesod, charge and black flame, added together
	public static int afterArmor( Hero hero, int dmg ){
		if (hero.heroClass == HeroClass.YESOD){
			dmg = Math.round(dmg * YesodTrackers.attackMultiplier(hero));
		}
		return dmg;
	}

	//damage dealt to a target without an attack roll, with the given armor roll subtracted (0 for none)
	public static int direct( Char attacker, Char target, int dmg, int armor ){
		if (!(attacker instanceof Hero) || dmg <= 0) return Math.max(0, dmg - armor);
		Hero hero = (Hero) attacker;
		float boosted = beforeArmor(hero, target, dmg);
		//G.O visualization +2: analyzed enemies take 20% more, the same as from an attack
		if (target != null && target.buff(Analyzed.class) != null && hero.pointsInTalent(Talent.GO_VISUALIZATION) >= 2){
			boosted *= 1.2f;
		}
		int result = Math.max(0, Math.round(boosted) - armor);
		return afterArmor(hero, result);
	}

	public static int direct( Char attacker, Char target, int dmg ){
		return direct(attacker, target, dmg, 0);
	}
}
