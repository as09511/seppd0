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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

//The language floor librarian: punches like a +0 ring of force, punches back when hit in melee.
//Charge: each tile walked adds 10% to the next melee hit (50% at most), lost after 2 turns without moving.
// At +2/+3 the action button dashes at an enemy 2/4 tiles away and hits it, 10 turn cooldown
public class LanguageLibrarian extends Buff implements ActionIndicator.Action {

	public static final int MAX_TILES = 5;
	public static final float BONUS_PER_TILE = 0.1f;
	public static final int DASH_COOLDOWN = 10;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private int tiles = 0;
	private int idle = 0;
	private int cooldown = 0;

	public static LanguageLibrarian get( Char ch ){
		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.LANGUAGE_LIBRARIAN){
			return Buff.affect(ch, LanguageLibrarian.class);
		}
		return null;
	}

	public static int dashRange( Hero hero ){
		switch (hero.pointsInTalent(Talent.LUNGE)){
			default: return 0;
			case 2: return 2;
			case 3: return 4;
		}
	}

	public void onStep(){
		if (!((Hero) target).hasTalent(Talent.LUNGE)) return;
		tiles = Math.min(MAX_TILES, tiles + 1);
		idle = 0;
		BuffIndicator.refreshHero();
	}

	//the next melee hit's bonus, used up by it
	public float useCharge(){
		float bonus = tiles * BONUS_PER_TILE;
		tiles = 0;
		BuffIndicator.refreshHero();
		return bonus;
	}

	@Override
	public boolean act() {
		Hero hero = (Hero) target;
		if (hero.subClass != HeroSubClass.LANGUAGE_LIBRARIAN){
			detach();
			return true;
		}
		if (cooldown > 0) cooldown--;
		if (tiles > 0){
			idle++;
			//a step this turn sets idle back to 0 before the next act
			if (idle > 2){
				tiles = 0;
				idle = 0;
				BuffIndicator.refreshHero();
			}
		}
		refreshAction();
		spend(TICK);
		return true;
	}

	private boolean dashReady(){
		return dashRange((Hero) target) > 0 && cooldown <= 0;
	}

	public void refreshAction(){
		if (dashReady()){
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
		return tiles > 0 ? BuffIndicator.MOMENTUM : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(1f, 0.4f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - tiles / (float)MAX_TILES);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(tiles);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round(tiles * BONUS_PER_TILE * 100));
	}

	@Override
	public String actionName() {
		return Messages.get(this, "action_name");
	}

	@Override
	public int actionIcon() {
		return HeroIcon.MOMENTUM;
	}

	@Override
	public int indicatorColor() {
		return 0x8B1A1A;
	}

	@Override
	public void doAction() {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell != null) dash(cell);
			}

			@Override
			public String prompt() {
				return Messages.get(LanguageLibrarian.class, "prompt", dashRange((Hero) target));
			}
		});
	}

	private void dash( int cell ){
		Hero hero = (Hero) target;
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !Dungeon.level.heroFOV[cell]){
			GLog.w(Messages.get(LanguageLibrarian.class, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, cell) > dashRange(hero)){
			GLog.w(Messages.get(LanguageLibrarian.class, "too_far"));
			return;
		}
		if (hero.rooted){
			PixelScene.shake(1, 1f);
			return;
		}

		//she runs straight at the enemy and stops on the last free tile before it
		int dest = hero.pos;
		if (!Dungeon.level.adjacent(hero.pos, cell)){
			Ballistica path = new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.STOP_CHARS);
			if (path.collisionPos != cell || path.dist < 1){
				GLog.w(Messages.get(LanguageLibrarian.class, "blocked"));
				return;
			}
			dest = path.path.get(path.dist - 1);
			if (!Dungeon.level.passable[dest] || (Actor.findChar(dest) != null && dest != hero.pos)){
				GLog.w(Messages.get(LanguageLibrarian.class, "blocked"));
				return;
			}
		}

		cooldown = DASH_COOLDOWN;
		refreshAction();
		int run = Dungeon.level.distance(hero.pos, dest);
		tiles = Math.min(MAX_TILES, tiles + run);
		idle = 0;

		final int to = dest;
		hero.busy();
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
		Callback strike = new Callback() {
			@Override
			public void call() {
				hero.sprite.attack(enemy.pos, new Callback() {
					@Override
					public void call() {
						hero.attack(enemy);
						Invisibility.dispel();
						hero.spendAndNext(Actor.TICK);
					}
				});
			}
		};
		if (to == hero.pos){
			strike.call();
		} else {
			hero.sprite.jump(hero.pos, to, 0, 0.12f, new Callback() {
				@Override
				public void call() {
					hero.move(to);
					Dungeon.level.occupyCell(hero);
					Dungeon.observe();
					GameScene.updateFog();
					strike.call();
				}
			});
		}
	}

	private static final String TILES = "tiles";
	private static final String IDLE = "idle";
	private static final String COOLDOWN = "cooldown";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(TILES, tiles);
		bundle.put(IDLE, idle);
		bundle.put(COOLDOWN, cooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		tiles = bundle.getInt(TILES);
		idle = bundle.getInt(IDLE);
		cooldown = bundle.getInt(COOLDOWN);
	}

	//vicious claws: hidden for 5 turns, the next attack in that time also bleeds
	public static class ClawReady extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.BLEEDING;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.3f, 0.3f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (5f - visualcooldown()) / 5f);
		}
	}

	public static class ClawCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.7f, 0.2f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, visualcooldown() / 100f);
		}
	}

	//shown on the hero when she punches back
	public static void showCounter( Hero hero ){
		if (hero.sprite != null) hero.sprite.showStatus(CharSprite.WARNING, Messages.get(LanguageLibrarian.class, "counter"));
	}
}
