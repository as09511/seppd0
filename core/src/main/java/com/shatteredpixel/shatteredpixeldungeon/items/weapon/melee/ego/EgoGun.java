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


package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//E.G.O guns: weak melee attacks, plus a magazine of bullets that are fired while equipped.
//Bullets count as thrown weapons, and the gun has to be reloaded once the magazine is empty.
public abstract class EgoGun extends EgoWeapon {

	public static final String AC_SHOOT  = "SHOOT";
	public static final String AC_RELOAD = "RELOAD";

	protected int ammo = maxAmmo();

	public abstract int maxAmmo();

	public abstract float reloadTime();

	public abstract int shotMin(int lvl);

	public abstract int shotMax(int lvl);

	//fires one bullet at the target cell, the gun has already used up the ammo
	//by default the bullet stops at the first character it reaches and uses normal thrown weapon accuracy
	protected void fireShot(Hero hero, int target){
		//shooting is affected by attack speed, just like melee attacks
		float delay = hero.attackDelay();
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.PROJECTILE);
		int cell = path.collisionPos;

		fireSound();
		Callback onArrival = new Callback() {
			@Override
			public void call() {
				Char ch = Actor.findChar(cell);
				if (ch != null && ch != hero){
					onShotHit(hero, ch, shoot(hero, ch));
				}
				hero.spendAndNext(delay);
			}
		};
		if (isBow()){
			((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(hero.sprite, cell, new ArrowVFX(), onArrival);
		} else {
			MagicMissile.boltFromChar(hero.sprite.parent, boltType(), hero.sprite, cell, onArrival);
		}
	}

	//the visual of the bullet in flight
	protected int boltType(){
		return MagicMissile.SHAMAN_RED;
	}

	//called after a bullet reaches a character, hit says whether it landed
	protected void onShotHit(Hero hero, Char ch, boolean hit){
	}

	//the damage type of a bullet that hits
	protected DamageType shotDamageType(){
		return damageType;
	}

	//bullet-only on-hit effects, after the damage type is applied
	protected int shotProc(Char attacker, Char defender, int damage){
		return damage;
	}

	//guns without a magazine never run out and cannot be reloaded
	protected boolean usesAmmo(){
		return true;
	}

	//guns that refill themselves cannot be reloaded by hand
	protected boolean canReload(){
		return true;
	}

	//firearms and bows sound different, everything added so far is a firearm
	protected boolean isBow(){
		return false;
	}

	//firearms sound like a throwing hammer hitting, bows like throwing a dart
	protected void fireSound(){
		if (isBow()){
			Sample.INSTANCE.play(Assets.Sounds.MISS, 0.6f, 0.6f, 1.5f);
		} else {
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.8f);
		}
	}

	//only bows make a sound on hit, the same as a dart hitting
	protected void shotHitSound(float pitch){
		if (isBow()){
			Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 1, pitch * 1.3f);
		}
	}

	public int ammo(){
		return ammo;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)){
			actions.add(AC_SHOOT);
			if (usesAmmo() && canReload()) actions.add(AC_RELOAD);
		}
		return actions;
	}

	@Override
	public String defaultAction() {
		return AC_SHOOT;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_SHOOT)){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (usesAmmo() && ammo <= 0){
				//trying to fire an empty gun reloads it instead
				if (canReload()) reload(hero);
				else GLog.w(Messages.get(this, "no_ammo"));
			} else {
				usesTargeting = true;
				GameScene.selectCell(shooter);
			}
		} else if (action.equals(AC_RELOAD) && usesAmmo() && canReload()){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (ammo >= maxAmmo()){
				GLog.i(Messages.get(this, "already_loaded"));
			} else {
				reload(hero);
			}
		}
	}

	protected void reload(Hero hero){
		refill(hero);
		hero.spendAndNext(reloadTime());
	}

	//refills the magazine without spending any time
	protected void refill(Hero hero){
		ammo = maxAmmo();
		updateQuickslot();
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		GLog.i(Messages.get(this, "reloaded"));
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell != null && cell != Dungeon.hero.pos) {
				Hero hero = Dungeon.hero;
				hero.busy();
				hero.sprite.zap(cell);
				if (usesAmmo()) {
					ammo--;
					updateQuickslot();
				}
				Invisibility.dispel();
				fireShot(hero, cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(EgoGun.this, "prompt");
		}
	};

	//hits a character with a bullet, which uses the normal thrown weapon accuracy
	protected boolean shoot(Hero hero, Char ch){
		return hero.shoot(ch, new Shot(this));
	}

	@Override
	public String status() {
		return usesAmmo() ? ammo + "/" + maxAmmo() : null;
	}

	@Override
	public String statsInfo() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc", augment.damageFactor(shotMin(lvl)), augment.damageFactor(shotMax(lvl)), damageTypeName());
	}

	//duelist ability: instantly reload
	@Override
	public String targetingPrompt() {
		return null;
	}

	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		beforeAbilityUsed(hero, null);
		refill(hero);
		afterAbilityUsed(hero);
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, "ability_desc", maxAmmo());
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return null;
	}

	private static final String AMMO = "ammo";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(AMMO, ammo);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(AMMO)) ammo = bundle.getInt(AMMO);
	}

	//a single bullet, which counts as a thrown weapon for things like armor and the sniper subclass
	public static class Shot extends MissileWeapon {

		private EgoGun gun;

		public Shot(){
			this(new MagicBullet());
		}

		public Shot(EgoGun gun){
			this.gun = gun;
			image = gun.image;
			tier = gun.tier;
			augment = gun.augment;
			enchantment = gun.enchantment;
		}

		@Override
		public int buffedLvl() {
			return gun.buffedLvl();
		}

		//sharpshooting only adds flat damage, it does not boost the bullet's level
		@Override
		public int min() {
			return gun.shotMin(buffedLvl());
		}

		@Override
		public int max() {
			return gun.shotMax(buffedLvl());
		}

		@Override
		public int min(int lvl) {
			return gun.shotMin(lvl);
		}

		@Override
		public int max(int lvl) {
			return gun.shotMax(lvl);
		}

		@Override
		public int STRReq(int lvl) {
			return gun.STRReq(lvl);
		}

		//the bullet itself is always +0, so it takes the gun's own requirement, upgrades and mastery included
		@Override
		public int STRReq() {
			return gun.STRReq();
		}

		@Override
		public int damageRoll(Char owner) {
			return super.damageRoll(owner) + RingOfSharpshooting.levelDamageBonus(owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			damage = DamageType.apply(defender, gun.shotDamageType(), damage);
			damage = gun.shotProc(attacker, defender, damage);
			return super.proc(attacker, defender, damage);
		}

		@Override
		public void hitSound(float pitch) {
			gun.shotHitSound(pitch);
		}
	}

	//only used to show an arrow in flight for bows
	public static class ArrowVFX extends Item {
		{
			image = ItemSpriteSheet.SPIRIT_ARROW;
		}
	}
}
