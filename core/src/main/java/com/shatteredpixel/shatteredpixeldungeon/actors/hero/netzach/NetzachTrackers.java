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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.netzach;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Netzach's tier 3 talents and subclass effects
public class NetzachTrackers {

	//safety team sephirah: natural regeneration 2x as fast, 3x at half health or less
	public static float regenDelayFactor( Hero hero ){
		if (hero.subClass != HeroSubClass.SAFETY_SEPHIRAH) return 1f;
		return hero.HP*2 <= hero.HT ? 1f/3f : 1f/2f;
	}

	//recovery efficiency: +10/20/30% to all healing except natural regeneration
	public static float healFactor( Hero hero ){
		return 1f + 0.1f*hero.pointsInTalent(Talent.RECOVERY_EFFICIENCY);
	}

	//safety secured: 10/20/30% less damage taken while paying back deferred damage (not the payments themselves)
	public static float damageTakenFactor( Hero hero, Object src ){
		if (!hero.hasTalent(Talent.SAFETY_SECURED) || src instanceof Viscosity.DeferedDamage
				|| hero.buff(Viscosity.DeferedDamage.class) == null) return 1f;
		return 1f - 0.1f*hero.pointsInTalent(Talent.SAFETY_SECURED);
	}

	//called after concentrated enkephalin is drunk
	public static void onEnkephalinDrunk( Hero hero ){
		//additional dose: a shield of 20/40/60% max health
		if (hero.hasTalent(Talent.ADDITIONAL_DOSE)){
			int shield = Math.round(hero.HT * 0.2f * hero.pointsInTalent(Talent.ADDITIONAL_DOSE));
			Buff.affect(hero, Barrier.class).incShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
		//HP bullet supply: visible allies heal 50/75/100% of their max health
		if (hero.hasTalent(Talent.HP_BULLET)){
			float ratio = 0.25f + 0.25f*hero.pointsInTalent(Talent.HP_BULLET);
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Char.Alignment.ALLY && m.isAlive() && Dungeon.level.heroFOV[m.pos]){
					int heal = Math.min(Math.round(m.HT * ratio), m.HT - m.HP);
					if (heal > 0){
						m.HP += heal;
						m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
				}
			}
		}
	}

	//collapse ampoule: a potion of healing thrown at an enemy deals 100/150/200% of what it would heal Netzach,
	//returns false if it should shatter as usual
	public static boolean collapseAmpoule( Char target ){
		Hero hero = Dungeon.hero;
		if (hero == null || target == null || target == hero || target.alignment != Char.Alignment.ENEMY
				|| !hero.hasTalent(Talent.COLLAPSE_AMPOULE)) return false;
		int heal = Math.round((0.8f * hero.HT + 14) * healFactor(hero));
		int damage = Math.round(heal * (0.5f + 0.5f*hero.pointsInTalent(Talent.COLLAPSE_AMPOULE)));
		target.damage(damage, hero);
		return true;
	}

	//art floor librarian: 25% chance on a hit to weaken, cripple or enrage the enemy for 3 turns.
	//incomprehensible: 40/50/50% chance, and at +3 half the time two of them. fervent emotion: +1/2/3 turns
	@SuppressWarnings("unchecked")
	private static final Class<? extends FlavourBuff>[] ART_DEBUFFS = new Class[]{Weakness.class, Cripple.class, Amok.class};

	public static void afterHit( Hero hero, Char enemy ){
		if (hero.subClass != HeroSubClass.ART_LIBRARIAN || !enemy.isAlive() || enemy.alignment == hero.alignment) return;

		int pts = hero.pointsInTalent(Talent.INCOMPREHENSIBLE);
		float chance = pts == 0 ? 0.25f : (pts == 1 ? 0.4f : 0.5f);
		if (Random.Float() >= chance) return;

		int count = (pts >= 3 && Random.Int(2) == 0) ? 2 : 1;
		float duration = 3f + hero.pointsInTalent(Talent.FERVENT_EMOTION);

		ArrayList<Class<? extends FlavourBuff>> debuffs = new ArrayList<>();
		for (Class<? extends FlavourBuff> c : ART_DEBUFFS) debuffs.add(c);
		for (int i = 0; i < count && !debuffs.isEmpty(); i++){
			Class<? extends FlavourBuff> c = debuffs.remove(Random.Int(debuffs.size()));
			Buff.prolong(enemy, c, duration);
		}
	}

	//finale: +15/30/45% damage against enemies with two or more debuffs
	public static float attackMultiplier( Hero hero, Char enemy ){
		if (enemy == null || !hero.hasTalent(Talent.FINALE)) return 1f;
		int debuffs = 0;
		for (Buff b : enemy.buffs()){
			if (b.type == Buff.buffType.NEGATIVE) debuffs++;
		}
		return debuffs >= 2 ? 1f + 0.15f*hero.pointsInTalent(Talent.FINALE) : 1f;
	}
}
