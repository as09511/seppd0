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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//WAW, cannot surprise attack (see Hero.canSurpriseAttack), but can charm an enemy in sight
//and deals extra damage to enemies charmed by the wielder
public class Lamp extends EgoWeapon {

	public static final String AC_CHARM = "CHARM";

	private static final float CHARM_DURATION = 10f;
	private static final int CHARM_COOLDOWN = 10;

	{
		image = ItemSpriteSheet.LAMP;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;

		risk = RiskLevel.WAW;
		damageType = DamageType.BLACK;
		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  6 +             //6 base, up from 4
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  30 +            //30 base, up from 25
				6*lvl;          //+6 per level
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		Charm charm = defender.buff(Charm.class);
		if (charm != null && charm.object == attacker.id()){
			damage = Math.round(damage * 1.25f);
		}
		return damage;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)){
			actions.add(AC_CHARM);
		}
		return actions;
	}

	//charming is used straight from the quickslot while equipped
	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && isEquipped(Dungeon.hero)){
			return AC_CHARM;
		}
		return super.defaultAction();
	}

	public boolean onCooldown(){
		return Dungeon.hero != null && Dungeon.hero.buff(CharmCooldown.class) != null;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_CHARM)){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (hero.buff(CharmCooldown.class) != null){
				GLog.w(Messages.get(this, "cooldown", hero.buff(CharmCooldown.class).left()));
			} else {
				usesTargeting = true;
				GameScene.selectCell(charmer);
			}
		}
	}

	private final CellSelector.Listener charmer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]){
				GLog.w(Messages.get(Lamp.class, "no_target"));
				return;
			}

			Buff.affect(ch, Charm.class, CHARM_DURATION).object = hero.id();
			ch.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
			Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			Buff.affect(hero, CharmCooldown.class).set(CHARM_COOLDOWN);
			updateQuickslot();

			hero.sprite.zap(cell);
			Invisibility.dispel();
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Lamp.class, "charm_prompt");
		}
	};

	@Override
	public String status() {
		if (Dungeon.hero == null) return null;
		CharmCooldown cd = Dungeon.hero.buff(CharmCooldown.class);
		return cd == null ? null : Integer.toString(cd.left());
	}

	@Override
	protected int abilityBoost(int lvl) {
		//same as the battle axe
		return 5 + Math.round(1.5f*lvl);
	}

	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Mace.heavyBlowAbility(hero, target, 1, dmgBoost, this);
	}

	//counts down every turn so the number on the item stays current, also shown as a buff icon
	public static class CharmCooldown extends Buff {

		private int left;

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 0.6f, 0.6f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (CHARM_COOLDOWN - left) / (float)CHARM_COOLDOWN);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", left);
		}

		//the first turn of the cooldown passes one turn from now
		public void set(int turns){
			left = turns;
			spend(TICK);
		}

		public int left(){
			return left;
		}

		@Override
		public boolean act() {
			left--;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			Item.updateQuickslot();
			return true;
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
		}
	}

}
