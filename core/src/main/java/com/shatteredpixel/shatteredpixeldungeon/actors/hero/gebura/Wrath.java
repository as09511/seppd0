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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

//The disciplinary team sephirah's wrath: +1 for every hit she lands (melee or ranged) and every hit she takes,
// +2.5% damage dealt each. At 20 or more she can go into a frenzy from the action button.
//Wrath goes down by 1 each turn with no enemy in sight, after the grace of our work, never below the
// floor of purpose of pain. The same action button fires the disposal bullet.
public class Wrath extends Buff implements ActionIndicator.Action {

	public static final int FRENZY_AT = 20;
	public static final float DAMAGE_PER_STACK = 0.025f;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private int stacks = 0;
	private int grace = 0;
	private int bulletCooldown = 0;

	public int stacks(){
		return stacks;
	}

	//purpose of pain: +4/7/10 to the most wrath, and 2/4/5 of it never goes away
	public static int maxStacks( Hero hero ){
		switch (hero.pointsInTalent(Talent.PURPOSE_OF_PAIN)){
			default: return FRENZY_AT;
			case 1: return FRENZY_AT + 4;
			case 2: return FRENZY_AT + 7;
			case 3: return FRENZY_AT + 10;
		}
	}

	public static int minStacks( Hero hero ){
		switch (hero.pointsInTalent(Talent.PURPOSE_OF_PAIN)){
			default: return 0;
			case 1: return 2;
			case 2: return 4;
			case 3: return 5;
		}
	}

	//our work: wrath waits 5/10/15 turns after the last enemy leaves her sight before it goes down
	public static int graceTurns( Hero hero ){
		return 5*hero.pointsInTalent(Talent.OUR_WORK);
	}

	//disposal bullet: 100/70/50 turns
	public static int bulletCooldown( Hero hero ){
		switch (hero.pointsInTalent(Talent.DISPOSAL_BULLET)){
			default: case 1: return 100;
			case 2: return 70;
			case 3: return 50;
		}
	}

	public static final int BULLET_DAMAGE = 150;

	public static Wrath get( Char ch ){
		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.DISCIPLINE_SEPHIRAH){
			return Buff.affect(ch, Wrath.class);
		}
		return null;
	}

	public void gain( int amount ){
		Hero hero = (Hero) target;
		if (hero.buff(Frenzy.class) != null) return;
		stacks = Math.min(maxStacks(hero), Math.max(stacks, minStacks(hero)) + amount);
		grace = graceTurns(hero);
		refreshAction();
		BuffIndicator.refreshHero();
	}

	public float damageBonus(){
		return stacks * DAMAGE_PER_STACK;
	}

	@Override
	public boolean act() {
		Hero hero = (Hero) target;
		if (hero.subClass != HeroSubClass.DISCIPLINE_SEPHIRAH){
			detach();
			return true;
		}
		if (bulletCooldown > 0) bulletCooldown--;

		if (hero.buff(Frenzy.class) == null){
			if (hero.visibleEnemies() > 0){
				grace = graceTurns(hero);
			} else if (grace > 0){
				grace--;
			} else if (stacks > minStacks(hero)){
				stacks--;
			}
			stacks = Math.max(stacks, minStacks(hero));
			stacks = Math.min(stacks, maxStacks(hero));
		}

		refreshAction();
		spend(TICK);
		return true;
	}

	//frenzy ends: wrath starts again from its floor
	public void onFrenzyEnd(){
		stacks = minStacks((Hero) target);
		grace = 0;
		refreshAction();
		BuffIndicator.refreshHero();
	}

	private boolean frenzyReady(){
		return stacks >= FRENZY_AT && target.buff(Frenzy.class) == null;
	}

	private boolean bulletReady(){
		return ((Hero) target).hasTalent(Talent.DISPOSAL_BULLET) && bulletCooldown <= 0;
	}

	public void refreshAction(){
		if (frenzyReady() || bulletReady()){
			if (ActionIndicator.action == null || ActionIndicator.action == this) ActionIndicator.setAction(this);
		} else {
			ActionIndicator.clearAction(this);
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on) refreshAction();
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	@Override
	public int icon() {
		return BuffIndicator.RAGE;
	}

	@Override
	public void tintIcon( Image icon ) {
		if (stacks >= FRENZY_AT) icon.hardlight(1f, 0.1f, 0.1f);
		else                     icon.hardlight(0.8f, 0.35f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - stacks / (float)FRENZY_AT);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String desc() {
		Hero hero = (Hero) target;
		String desc = Messages.get(this, "desc", stacks, maxStacks(hero), Math.round(damageBonus()*1000)/10f);
		if (stacks >= FRENZY_AT && hero.buff(Frenzy.class) == null){
			desc += "\n\n" + Messages.get(this, "desc_frenzy", Math.round(stacks*Frenzy.SHIELD_PER_STACK*100), stacks/2);
		}
		if (hero.hasTalent(Talent.DISPOSAL_BULLET)){
			desc += "\n\n" + (bulletCooldown > 0
					? Messages.get(this, "desc_bullet_cooldown", bulletCooldown)
					: Messages.get(this, "desc_bullet_ready"));
		}
		return desc;
	}

	@Override
	public String actionName() {
		if (frenzyReady() && bulletReady()) return Messages.get(this, "action_both");
		if (frenzyReady()) return Messages.get(this, "action_frenzy");
		return Messages.get(this, "action_bullet");
	}

	@Override
	public int actionIcon() {
		return frenzyReady() ? HeroIcon.FRENZY : HeroIcon.DISPOSAL_BULLET;
	}

	@Override
	public int indicatorColor() {
		return 0x8B1A1A;
	}

	@Override
	public void doAction() {
		if (frenzyReady() && bulletReady()){
			GameScene.show(new WndOptions(new HeroIcon(this),
					Messages.titleCase(Messages.get(this, "name")),
					Messages.get(this, "choose"),
					Messages.get(this, "action_frenzy"),
					Messages.get(this, "action_bullet")){
				@Override
				protected void onSelect( int index ) {
					if (index == 0) startFrenzy();
					else if (index == 1) aimBullet();
				}
			});
		} else if (frenzyReady()){
			startFrenzy();
		} else if (bulletReady()){
			aimBullet();
		}
	}

	private void startFrenzy(){
		Hero hero = (Hero) target;
		if (!frenzyReady()) return;
		Frenzy frenzy = Buff.affect(hero, Frenzy.class);
		frenzy.set(stacks, hero);
		stacks = 0;
		grace = 0;
		hero.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Frenzy.class, "name"));
		hero.sprite.emitter().burst(Speck.factory(Speck.UP), 8);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		GameScene.flash(0x660000);
		refreshAction();
		BuffIndicator.refreshHero();
		hero.spendAndNext(Actor.TICK);
	}

	private void aimBullet(){
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				fireBullet(cell);
			}

			@Override
			public String prompt() {
				return Messages.get(Wrath.class, "bullet_prompt");
			}
		});
	}

	private void fireBullet( int cell ){
		Hero hero = (Hero) target;
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment
				|| !Dungeon.level.heroFOV[cell] || !bulletReady()){
			GLog.w(Messages.get(Wrath.class, "bullet_no_target"));
			return;
		}
		bulletCooldown = bulletCooldown(hero);
		refreshAction();
		hero.busy();
		hero.sprite.turnTo(hero.pos, cell);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHAMAN_RED, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				if (enemy.isAlive()){
					//fixed damage, no armor or damage bonuses
					enemy.damage(BULLET_DAMAGE, hero);
				}
				Invisibility.dispel();
				BuffIndicator.refreshHero();
				hero.spendAndNext(Actor.TICK);
			}
		});
	}

	private static final String STACKS = "stacks";
	private static final String GRACE = "grace";
	private static final String BULLET = "bullet_cooldown";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
		bundle.put(GRACE, grace);
		bundle.put(BULLET, bulletCooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
		grace = bundle.getInt(GRACE);
		bulletCooldown = bundle.getInt(BULLET);
	}

	//the frenzy: shielding of 3% max health per wrath spent, +100% damage dealt, for wrath/2 turns.
	// The shielding doesn't wear off on its own, it is gone when the frenzy ends
	public static class Frenzy extends ShieldBuff {

		public static final float SHIELD_PER_STACK = 0.03f;
		public static final float DAMAGE_BONUS = 1f;

		{
			type = buffType.POSITIVE;
			detachesAtZero = false;
		}

		private int left;
		private int duration;

		public void set( int wrath, Hero hero ){
			duration = left = Math.max(1, wrath/2);
			setShield(Math.round(hero.HT * wrath * SHIELD_PER_STACK));
		}

		@Override
		public boolean act() {
			left--;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public void detach() {
			super.detach();
			Wrath w = target.buff(Wrath.class);
			if (w != null) w.onFrenzyEnd();
		}

		@Override
		public void fx( boolean on ) {
			if (on) target.sprite.aura(0xFF2020, 6);
			else target.sprite.clearAura();
		}

		@Override
		public int icon() {
			return BuffIndicator.BERSERK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.15f, 0.1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - left / (float)Math.max(1, duration));
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", shielding(), left);
		}

		private static final String LEFT = "left";
		private static final String DURATION = "duration";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(DURATION, duration);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			duration = bundle.getInt(DURATION);
		}
	}
}
