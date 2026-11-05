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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.tiphereth;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BlazingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BurningTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ShockingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.StormTrap;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

//Tiphereth's tier 1 and 2 talents
public class TipherethTrackers {

	//pride: +1/+2 melee damage while two or more enemies are in sight
	public static int prideBonus( Hero hero ){
		if (!hero.hasTalent(Talent.PRIDE) || hero.visibleEnemies() < 2) return 0;
		return hero.pointsInTalent(Talent.PRIDE);
	}

	//preemptive defense: 3/5 shielding the first time each enemy lands a hit
	public static void onHitBy( Hero hero, Char enemy ){
		if (!hero.hasTalent(Talent.PREEMPTIVE_DEFENSE) || !(enemy instanceof Mob)) return;
		if (enemy.buff(PreemptiveMark.class) != null) return;
		Buff.affect(enemy, PreemptiveMark.class);
		int shield = 1 + 2*hero.pointsInTalent(Talent.PREEMPTIVE_DEFENSE);
		Buff.affect(hero, Barrier.class).incShield(shield);
		if (hero.sprite != null){
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	//marks an enemy that has already set off preemptive defense
	public static class PreemptiveMark extends Buff {
		{
			revivePersists = true;
		}
	}

	//sneaky: a surprise attack cripples for 2/3 turns
	public static void onAttack( Hero hero, Char enemy ){
		if (!hero.hasTalent(Talent.SNEAKY) || !(enemy instanceof Mob) || !enemy.isAlive()) return;
		if (((Mob) enemy).surprisedBy(hero, true)){
			Buff.prolong(enemy, Cripple.class, 1 + hero.pointsInTalent(Talent.SNEAKY));
		}
	}

	//expectation: every 5th/4th attack deals its maximum damage. Counts the attack and says whether this one does.
	public static boolean expectationHit( Hero hero ){
		if (!hero.hasTalent(Talent.EXPECTATION)) return false;
		ExpectationCounter c = Buff.affect(hero, ExpectationCounter.class);
		c.countUp(1);
		int every = 6 - hero.pointsInTalent(Talent.EXPECTATION);
		if (c.count() >= every){
			c.countDown(c.count());
			return true;
		}
		return false;
	}

	public static class ExpectationCounter extends CounterBuff {
		{
			revivePersists = true;
		}
	}

	//physical interference shield: +0~2/0~4 armor
	public static int physicalShield( Char ch ){
		Hero hero = owner(ch);
		if (hero == null || !hero.hasTalent(Talent.PHYSICAL_SHIELD)) return 0;
		return com.watabou.utils.Random.NormalIntRange(0, 2*hero.pointsInTalent(Talent.PHYSICAL_SHIELD));
	}

	//trauma prevention shield: elemental damage is reduced by 15/25% of a roll of the armor
	public static int traumaShield( Char ch, int dmg, Object src ){
		Hero hero = owner(ch);
		if (hero == null || !hero.hasTalent(Talent.TRAUMA_SHIELD) || !elemental(src) || dmg <= 0) return dmg;
		float ratio = 0.05f + 0.1f*hero.pointsInTalent(Talent.TRAUMA_SHIELD);
		int reduce = Math.round(ch.drRoll() * ratio);
		return Math.max(0, dmg - reduce);
	}

	//the hero these talents belong to: Tiphereth herself (Tiphereth B joins in with stage 2)
	private static Hero owner( Char ch ){
		if (ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.TIPHERETH) return (Hero) ch;
		return null;
	}

	//fire, frost and lightning, whatever they come from
	public static boolean elemental( Object src ){
		return src instanceof Burning || src instanceof Fire || src instanceof Inferno
				|| src instanceof Frost || src instanceof Freezing || src instanceof Blizzard
				|| src instanceof Electricity
				|| src instanceof WandOfFireblast || src instanceof WandOfFrost || src instanceof WandOfLightning
				|| src instanceof Blazing || src instanceof Shocking
				|| src instanceof BurningTrap || src instanceof BlazingTrap
				|| src instanceof ShockingTrap || src instanceof StormTrap;
	}
}
