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

import com.watabou.utils.Random;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPrismaticImage;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import java.util.Collections;
import java.util.HashSet;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
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

	//preemptive defense: 3/5 shielding the first time each kind of enemy lands a hit,
	// tracked per kind for the whole run like Yesod's new enemy info
	public static void onHitBy( Hero hero, Char enemy ){
		if (!hero.hasTalent(Talent.PREEMPTIVE_DEFENSE) || !(enemy instanceof Mob)) return;
		if (!Buff.affect(hero, PreemptiveTypes.class).types.add(enemy.getClass().getName())) return;
		int shield = 1 + 2*hero.pointsInTalent(Talent.PREEMPTIVE_DEFENSE);
		Buff.affect(hero, Barrier.class).incShield(shield);
		if (hero.sprite != null){
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	//the kinds of enemy that have already set off preemptive defense
	public static class PreemptiveTypes extends Buff {
		{
			revivePersists = true;
		}

		public final HashSet<String> types = new HashSet<>();

		private static final String TYPES = "types";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(TYPES, types.toArray(new String[0]));
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			types.clear();
			if (bundle.contains(TYPES)) Collections.addAll(types, bundle.getStringArray(TYPES));
		}
	}

	public static void onAttack( Hero hero, Char enemy ){
		onUnarmedHit(hero, enemy);
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

	//trauma prevention shield: magic damage is reduced by 15/25% of a roll of the armor
	public static int traumaShield( Char ch, int dmg, Object src ){
		Hero hero = owner(ch);
		if (hero == null || !hero.hasTalent(Talent.TRAUMA_SHIELD) || !elemental(src) || dmg <= 0) return dmg;
		float ratio = 0.05f + 0.1f*hero.pointsInTalent(Talent.TRAUMA_SHIELD);
		int reduce = Math.round(ch.drRoll() * ratio);
		return Math.max(0, dmg - reduce);
	}

	//the hero these talents belong to: Tiphereth herself, or the Tiphereth she called as Tiphereth B
	private static Hero owner( Char ch ){
		if (ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.TIPHERETH) return (Hero) ch;
		if (ch instanceof TipherethB) return ((TipherethB) ch).owner();
		return null;
	}

	//any magic damage: whatever the anti-magic glyph resists, plus fire, frost and lightning from any source
	public static boolean elemental( Object src ){
		if (src == null) return false;
		for (Class<?> c : AntiMagic.RESISTS){
			if (c.isAssignableFrom(src.getClass())) return true;
		}
		return src instanceof Burning || src instanceof Fire || src instanceof Inferno
				|| src instanceof Frost || src instanceof Freezing || src instanceof Blizzard
				|| src instanceof Electricity
				|| src instanceof WandOfFireblast || src instanceof WandOfFrost || src instanceof WandOfLightning
				|| src instanceof Blazing || src instanceof Shocking
				|| src instanceof BurningTrap || src instanceof BlazingTrap
				|| src instanceof ShockingTrap || src instanceof StormTrap;
	}

	//the HQ sephirah gets a scroll of prismatic image as soon as she takes the subclass
	public static void onSubclassChosen( Hero hero ){
		if (hero.subClass == HeroSubClass.HQ_SEPHIRAH){
			ScrollOfPrismaticImage scroll = new ScrollOfPrismaticImage();
			scroll.identify();
			if (!scroll.collect()) Dungeon.level.drop(scroll, hero.pos).sprite.drop();
		}
	}

	//not a kid!: +10/15/20% evasion
	public static float evasionFactor( Hero hero ){
		if (!hero.hasTalent(Talent.NOT_A_KID)) return 1f;
		return 1.05f + 0.05f*hero.pointsInTalent(Talent.NOT_A_KID);
	}

	//erosion neutralizing shield: debuffs on Tiphereth and Tiphereth B last 20/30/40% shorter
	public static float debuffDuration( Char target, Buff buff ){
		if (buff == null || buff.type != Buff.buffType.NEGATIVE) return 1f;
		Hero hero = target instanceof TipherethB ? ((TipherethB) target).owner() : owner(target);
		if (hero == null || !hero.hasTalent(Talent.EROSION_SHIELD)) return 1f;
		return 0.9f - 0.1f*hero.pointsInTalent(Talent.EROSION_SHIELD);
	}

	//Tiphereth B takes part of the health damage Tiphereth takes, shielding is used up first and isn't shared.
	//Returns the damage Tiphereth still takes herself.
	public static int shareWithB( Hero hero, int dmg, Object src ){
		if (hero.heroClass != HeroClass.TIPHERETH || dmg <= 0 || src instanceof Hunger) return dmg;
		TipherethB b = TipherethB.get();
		if (b == null || b.owner() != hero) return dmg;
		int toHealth = Math.max(0, dmg - hero.shielding());
		int share = Math.min(Math.round(toHealth * TipherethB.shareRatio(hero)), b.HP);
		if (share <= 0) return dmg;
		b.damage(share, src);
		return dmg - share;
	}

	//ward: the next damage taken is halved, ready again 10/7/5 turns after it is used
	public static float wardFactor( Hero hero ){
		if (!hero.hasTalent(Talent.WARD)) return 1f;
		Ward w = Buff.affect(hero, Ward.class);
		if (w.cooldown > 0) return 1f;
		w.cooldown = WARD_COOLDOWN[Math.min(3, hero.pointsInTalent(Talent.WARD))];
		BuffIndicator.refreshHero();
		return 0.5f;
	}

	private static final int[] WARD_COOLDOWN = {0, 10, 7, 5};

	public static class Ward extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public int cooldown = 0;

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !((Hero) target).hasTalent(Talent.WARD)){
				detach();
				return true;
			}
			if (cooldown > 0){
				cooldown--;
				if (cooldown == 0) BuffIndicator.refreshHero();
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.WARD;
		}

		@Override
		public float iconFadePercent() {
			Hero hero = (Hero) target;
			int max = WARD_COOLDOWN[Math.min(3, hero.pointsInTalent(Talent.WARD))];
			return max <= 0 ? 0 : cooldown / (float) max;
		}

		@Override
		public String iconTextDisplay() {
			return cooldown > 0 ? Integer.toString(cooldown) : "";
		}

		@Override
		public String desc() {
			return cooldown > 0 ? Messages.get(this, "desc_cooldown", cooldown) : Messages.get(this, "desc_ready");
		}

		private static final String COOLDOWN = "cooldown";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(COOLDOWN, cooldown);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cooldown = bundle.getInt(COOLDOWN);
		}
	}

	//intemperance: unarmed attacks paralyse for 1 turn 15/20/25% of the time
	public static void onUnarmedHit( Hero hero, Char enemy ){
		if (!hero.hasTalent(Talent.INTEMPERANCE) || !enemy.isAlive() || !RingOfForce.fightingUnarmed(hero)) return;
		if (Random.Float() < 0.1f + 0.05f*hero.pointsInTalent(Talent.INTEMPERANCE)){
			Buff.prolong(enemy, Paralysis.class, 1f);
		}
	}

	//emptiness: each empty artifact or ring slot, 10/17.5/25% more damage dealt and 5/10/15% less taken
	public static int emptySlots( Hero hero ){
		if (!hero.hasTalent(Talent.EMPTINESS)) return 0;
		int n = 0;
		if (hero.belongings.artifact() == null) n++;
		if (hero.belongings.misc() == null) n++;
		if (hero.belongings.ring() == null) n++;
		return n;
	}

	public static float emptinessDealt( Hero hero ){
		return 1f + emptySlots(hero) * (0.025f + 0.075f*hero.pointsInTalent(Talent.EMPTINESS));
	}

	public static float emptinessTaken( Hero hero ){
		return Math.max(0f, 1f - emptySlots(hero) * 0.05f*hero.pointsInTalent(Talent.EMPTINESS));
	}
}
