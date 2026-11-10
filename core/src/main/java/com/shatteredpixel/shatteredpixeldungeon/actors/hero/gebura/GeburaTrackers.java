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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.watabou.noosa.audio.Sample;
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

	//proof of strength: +1 strength. Kiai: +8/13/18% of her base strength, rounded down like the warrior's strongman
	public static int strBonus( Hero hero ){
		int bonus = hero.hasTalent(Talent.PROOF_OF_STRENGTH) ? 1 : 0;
		if (hero.hasTalent(Talent.KIAI)){
			bonus += (int)Math.floor(hero.STR * (0.03f + 0.05f*hero.pointsInTalent(Talent.KIAI)));
		}
		return bonus;
	}

	//damage dealt bonuses on every kind of her damage: blade +10/20/30%, wrath +2.5% each, frenzy +100%
	public static float percentBonus( Hero hero, Char target ){
		float bonus = 0.1f*hero.pointsInTalent(Talent.BLADE);
		Wrath wrath = hero.buff(Wrath.class);
		if (wrath != null) bonus += wrath.damageBonus();
		if (hero.buff(Wrath.Frenzy.class) != null) bonus += Wrath.Frenzy.DAMAGE_BONUS;
		return bonus;
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
		//the language floor librarian's charge, on melee attacks that aren't counters
		LanguageLibrarian lib = hero.buff(LanguageLibrarian.class);
		if (lib != null && !counterAttacking && !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			bonus += lib.useCharge();
		}
		return bonus;
	}

	//after a hit lands
	public static void afterHit( Hero hero, Char enemy, int damage ){
		//the disciplinary team sephirah's wrath, from melee and ranged hits alike
		Wrath wrath = Wrath.get(hero);
		if (wrath != null) wrath.gain(1);

		//vicious claws: the first attack while hidden also bleeds for 33/67/100% of its damage
		LanguageLibrarian.ClawReady claw = hero.buff(LanguageLibrarian.ClawReady.class);
		if (claw != null){
			claw.detach();
			int bleed = Math.round(damage * hero.pointsInTalent(Talent.VICIOUS_CLAW) / 3f);
			if (bleed > 0 && enemy.isAlive()) Buff.affect(enemy, Bleeding.class).set(bleed);
		}

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

	//E.G.O mastery: +1 weapons are identified when wielded, +2 the level of weapons is also known on pickup
	public static boolean identifiesOnEquip( Hero hero, Item item ){
		return hero.hasTalent(Talent.EGO_MASTERY) && item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.pointsInTalent(Talent.EGO_MASTERY) >= 2 && !ShardOfOblivion.passiveIDDisabled()
				&& item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon){
			item.levelKnown = true;
		}
	}

	//after she takes damage
	public static void onDamaged( Hero hero, Object src, int preHP ){
		if (!(src instanceof Char) || src == hero) return;
		Wrath wrath = Wrath.get(hero);
		if (wrath != null) wrath.gain(1);

		//vicious claws: a hit that takes her to half health or less hides her for 5 turns, 100 turn cooldown
		if (hero.hasTalent(Talent.VICIOUS_CLAW) && hero.isAlive()
				&& hero.HP <= hero.HT/2 && preHP > hero.HT/2
				&& hero.buff(LanguageLibrarian.ClawCooldown.class) == null){
			Buff.affect(hero, LanguageLibrarian.ClawCooldown.class, 100f);
			Buff.prolong(hero, Invisibility.class, 5f);
			Buff.prolong(hero, LanguageLibrarian.ClawReady.class, 5f);
			Sample.INSTANCE.play(Assets.Sounds.MELD);
		}
	}

	//absorption: 2/4/6 health back on every kill
	public static void onEnemySlain( Hero hero ){
		if (hero.hasTalent(Talent.ABSORPTION) && hero.isAlive()){
			int heal = Math.min(2*hero.pointsInTalent(Talent.ABSORPTION), hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	//the language floor librarian punches back at whoever hits her in melee, right after their attack
	public static boolean counterAttacking = false;

	public static void onHitBy( Hero hero, Char enemy ){
		if (hero.subClass != HeroSubClass.LANGUAGE_LIBRARIAN || enemy == null || enemy == hero) return;
		if (!Dungeon.level.adjacent(hero.pos, enemy.pos)) return;
		Actor.add(new Actor() {
			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act() {
				Actor.remove(this);
				if (hero.isAlive() && enemy.isAlive() && hero.paralysed <= 0
						&& Dungeon.level.adjacent(hero.pos, enemy.pos) && enemy.alignment != hero.alignment){
					LanguageLibrarian.showCounter(hero);
					counterAttacking = true;
					try {
						hero.attack(enemy);
					} finally {
						counterAttacking = false;
					}
				}
				return true;
			}
		});
	}

	public static void onStep( Hero hero ){
		LanguageLibrarian lib = LanguageLibrarian.get(hero);
		if (lib != null) lib.onStep();
	}

	//the subclass buffs, kept on the hero
	public static void checkBuffs( Hero hero ){
		if (hero.subClass == HeroSubClass.DISCIPLINE_SEPHIRAH) Wrath.get(hero);
		if (hero.subClass == HeroSubClass.LANGUAGE_LIBRARIAN) LanguageLibrarian.get(hero);
	}

	//proof of strength +2: cursed equipment of +2 or less can be taken off by force, which destroys it
	public static boolean canForceRemove( Hero hero, EquipableItem item ){
		return item.cursed && hero.pointsInTalent(Talent.PROOF_OF_STRENGTH) >= 2 && item.trueLevel() <= 2;
	}
}
