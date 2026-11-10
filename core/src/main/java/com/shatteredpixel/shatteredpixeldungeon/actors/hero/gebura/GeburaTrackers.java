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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.gebura;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Cigarette;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//Gebura's base stats and her tier 1 and 2 talents
public class GeburaTrackers {

	//Gebura starts with 10 more max health than the other sephirot, legend adds 10/20% on top
	public static int bonusHT( Hero hero ){
		return hero.heroClass == HeroClass.GEBURA ? 10 : 0;
	}

	public static float htMultiplier( Hero hero ){
		return 1f + 0.1f*hero.pointsInTalent(Talent.LEGEND);
	}

	//proof of strength: +1 strength
	public static int strBonus( Hero hero ){
		return hero.hasTalent(Talent.PROOF_OF_STRENGTH) ? 1 : 0;
	}

	//fixer: +2/+3 accuracy
	public static int accuracyBonus( Hero hero ){
		return hero.hasTalent(Talent.FIXER) ? 1 + hero.pointsInTalent(Talent.FIXER) : 0;
	}

	//cigarette time: a cigarette for each point
	public static void giveCigarette( Hero hero ){
		Item cig = new Cigarette();
		if (!cig.collect()) Dungeon.level.drop(cig, hero.pos).sprite.drop();
		Item.updateQuickslot();
	}

	//indomitable: a hit that would take her from 2 or more health to 0 leaves her at 1 instead,
	// at +2 she is also invulnerable for a turn. 100 turn cooldown
	public static final float INDOMITABLE_COOLDOWN = 100f;

	public static int indomitable( Hero hero, int dmg, Object src ){
		if (!hero.hasTalent(Talent.INDOMITABLE) || dmg <= 0 || hero.HP < 2) return dmg;
		if (hero.buff(IndomitableCooldown.class) != null) return dmg;
		int lethal = hero.HP + (src instanceof Hunger ? 0 : hero.shielding());
		if (dmg < lethal) return dmg;

		Buff.affect(hero, IndomitableCooldown.class, INDOMITABLE_COOLDOWN);
		if (hero.pointsInTalent(Talent.INDOMITABLE) >= 2){
			Buff.prolong(hero, Invulnerability.class, 1f);
		}
		if (hero.sprite != null){
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(GeburaTrackers.class, "indomitable"));
		}
		return lethal - 1;
	}

	public static class IndomitableCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.9f, 0.15f, 0.15f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, visualcooldown() / INDOMITABLE_COOLDOWN);
		}
	}

	//after-meal smoke: 1 turn meals, and the next 1/2 attacks deal 30% more damage
	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.AFTER_MEAL_SMOKE)){
			Buff.affect(hero, AfterMealSmoke.class).set(hero.pointsInTalent(Talent.AFTER_MEAL_SMOKE));
		}
	}

	public static class AfterMealSmoke extends Buff {
		{
			type = buffType.POSITIVE;
		}

		private int left;

		public void set( int attacks ){
			left = Math.max(left, attacks);
		}

		//counts one attack and gives its bonus
		public float use(){
			left--;
			if (left <= 0) detach();
			return 0.3f;
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAPON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.45f, 0.2f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", left);
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
		}
	}

	//percent damage dealt bonuses that only an attack gets, used up by it
	public static float attackBonus( Hero hero ){
		float bonus = 0f;
		AfterMealSmoke meal = hero.buff(AfterMealSmoke.class);
		if (meal != null) bonus += meal.use();
		return bonus;
	}

	//after a hit lands
	public static void afterHit( Hero hero, Char enemy ){
		//qliphoth interference field: ranged hits cripple for 2/3 turns, 10 turn cooldown
		if (hero.hasTalent(Talent.QLIPHOTH_FIELD) && enemy.isAlive()
				&& hero.belongings.attackingWeapon() instanceof MissileWeapon
				&& hero.buff(QliphothFieldCooldown.class) == null){
			Buff.prolong(enemy, Cripple.class, 1 + hero.pointsInTalent(Talent.QLIPHOTH_FIELD));
			Buff.affect(hero, QliphothFieldCooldown.class, QLIPHOTH_COOLDOWN);
		}
	}

	public static final float QLIPHOTH_COOLDOWN = 10f;

	public static class QliphothFieldCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.6f, 0.3f, 0.9f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, visualcooldown() / QLIPHOTH_COOLDOWN);
		}
	}

	//the strongest: killing an enemy with a melee attack takes no time, at +2 it also gives a turn of adrenaline
	public static boolean strongestKill( Hero hero, Char target, boolean hit, boolean wasEnemy ){
		if (!hero.hasTalent(Talent.STRONGEST) || !hit || !wasEnemy || target.isAlive()) return false;
		if (hero.belongings.attackingWeapon() instanceof MissileWeapon) return false;
		if (hero.pointsInTalent(Talent.STRONGEST) >= 2){
			Buff.affect(hero, Adrenaline.class, 1f);
		}
		return true;
	}

	//E.G.O mastery: +1 weapons are identified when wielded, +2 the level of weapons and armor is known on pickup
	public static boolean identifiesOnEquip( Hero hero, Item item ){
		return hero.hasTalent(Talent.EGO_MASTERY) && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.pointsInTalent(Talent.EGO_MASTERY) >= 2 && !ShardOfOblivion.passiveIDDisabled()
				&& (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
					|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor)){
			item.levelKnown = true;
		}
	}

	//proof of strength +2: cursed equipment of +2 or less can be taken off by force, which destroys it
	public static boolean canForceRemove( Hero hero, EquipableItem item ){
		return item.cursed && hero.pointsInTalent(Talent.PROOF_OF_STRENGTH) >= 2 && item.trueLevel() <= 2;
	}
}
