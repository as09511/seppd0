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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//Hod's tier 3 talents and subclass effects
public class HodTrackers {

	//training team sephirah: max level 40 instead of 30
	public static final int TRAINING_MAX_LEVEL_BONUS = 10;

	public static int maxLevel( Hero hero ){
		return Hero.MAX_LEVEL + (hero.subClass == HeroSubClass.TRAINING_SEPHIRAH ? TRAINING_MAX_LEVEL_BONUS : 0);
	}

	//training team sephirah: enemies give exp for 5 more levels, 7/9/11 with the certificate talent
	public static int expCapBonus( Hero hero ){
		if (hero == null || hero.subClass != HeroSubClass.TRAINING_SEPHIRAH) return 0;
		return 5 + 2*hero.pointsInTalent(Talent.CERTIFICATE);
	}

	//potential development: +1 level per 15 stacks to the equipped weapon / and armor / and rings
	public static int potentialBonus( Item item ){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.heroClass != HeroClass.HOD || !hero.hasTalent(Talent.POTENTIAL_DEV)) return 0;
		if (item instanceof MissileWeapon || !item.isEquipped(hero)) return 0;
		int pts = hero.pointsInTalent(Talent.POTENTIAL_DEV);
		boolean applies = item instanceof KindOfWeapon
				|| (pts >= 2 && item instanceof Armor)
				|| (pts >= 3 && item instanceof Ring);
		return applies ? HodManual.stacks(hero) / 15 : 0;
	}

	//training results: +0.5/1/1.5% melee damage per stack
	//literature floor librarian: +20% damage against rooted enemies
	public static float attackMultiplier( Hero hero, Char enemy ){
		float mult = 1f;
		if (hero.hasTalent(Talent.TRAINING_RESULT) && !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			mult *= 1f + HodManual.stacks(hero) * 0.005f * hero.pointsInTalent(Talent.TRAINING_RESULT);
		}
		if (hero.subClass == HeroSubClass.LITERATURE_LIBRARIAN && enemy != null && enemy.buff(Roots.class) != null){
			mult *= 1.2f;
		}
		return mult;
	}

	//clumsy goodwill: +1 max armor per 4/3/2 stacks
	public static int armorBonus( Hero hero ){
		if (hero.heroClass != HeroClass.HOD || !hero.hasTalent(Talent.CLUMSY_GOODWILL)) return 0;
		return HodManual.stacks(hero) / (5 - hero.pointsInTalent(Talent.CLUMSY_GOODWILL));
	}

	//hope it helps: 2/3/4 turns of invulnerability after a level up or a potion of experience
	public static void onLevelUpOrExperience( Hero hero ){
		if (hero.hasTalent(Talent.HOPE_IT_HELPS)){
			Buff.prolong(hero, Invulnerability.class, 1 + hero.pointsInTalent(Talent.HOPE_IT_HELPS));
		}
	}

	//literature floor librarian's bleeding and the cocoon talent, applied after the hit lands
	public static void afterHit( Hero hero, Char enemy ){
		if (hero.subClass != HeroSubClass.LITERATURE_LIBRARIAN || !enemy.isAlive() || enemy.alignment == hero.alignment) return;

		//half the stacks as bleeding, obsession: +33/66/100%
		float bleed = HodManual.stacks(hero) / 2f * (1f + hero.pointsInTalent(Talent.OBSESSION) / 3f);
		if (bleed >= 1f){
			Buff.affect(enemy, Bleeding.class).set(bleed);
		}

		//cocoon: 1 turn of paralysis, 2 turns of roots and vulnerable, 20/15/10 turn cooldown
		if (hero.hasTalent(Talent.COCOON) && hero.buff(CocoonCooldown.class) == null){
			Buff.prolong(enemy, Paralysis.class, 1f);
			Buff.prolong(enemy, Roots.class, 2f);
			Buff.prolong(enemy, Vulnerable.class, 2f);
			Buff.affect(hero, CocoonCooldown.class, CocoonCooldown.duration(hero));
		}
	}

	//old umbrella: 10/12.5/15% chance to reflect a hit back at the attacker without taking it
	public static boolean reflects( Hero hero, Char enemy, int damage ){
		if (damage <= 0 || enemy == null || enemy == hero || !hero.hasTalent(Talent.OLD_UMBRELLA)) return false;
		if (com.watabou.utils.Random.Float() >= 0.075f + 0.025f*hero.pointsInTalent(Talent.OLD_UMBRELLA)) return false;
		if (hero.sprite != null){
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(HodTrackers.class, "reflect"));
		}
		enemy.damage(damage, hero);
		return true;
	}

	public static class CocoonCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}

		public static float duration( Hero hero ){
			return 25 - 5*hero.pointsInTalent(Talent.COCOON);
		}

		@Override
		public int icon() { return BuffIndicator.TIME; }

		@Override
		public void tintIcon(Image icon) { icon.hardlight(0.75f, 0.1f, 0.15f); }

		@Override
		public float iconFadePercent() {
			float max = Dungeon.hero == null ? 20 : duration(Dungeon.hero);
			return Math.max(0, visualcooldown() / max);
		}

		@Override
		public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }

		@Override
		public String desc() { return Messages.get(this, "desc", dispTurns(visualcooldown())); }
	}
}
