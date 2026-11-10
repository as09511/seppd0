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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.gebura;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.gebura.GeburaTrackers;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
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

import java.util.ArrayList;

//Gebura's second armor ability: the Red Mist returns. For 10 turns she moves twice as fast, deals 50% more
// damage and takes 20% less, and can use each great split once from the action button.
//Great split - vertical: a sure melee hit for 3x damage that paralyses for a turn.
//Great split - horizontal: 1.5x damage to every enemy in a half circle of radius 5 in the chosen direction.
//E.G.O sensitivity: 13/16/18/20 turns. Blood mist: +62.5/75/87.5/100% damage. Courage: -25/30/35/40% damage taken.
public class RedMist extends ArmorAbility {

	{
		baseChargeUse = 100f;
	}

	private static final int[] DURATION = {10, 13, 16, 18, 20};

	public static int duration( Hero hero ){
		return DURATION[Math.min(4, hero.pointsInTalent(Talent.EGO_SENSITIVITY))];
	}

	public static float damageBonus( Hero hero ){
		return 0.5f + 0.125f*hero.pointsInTalent(Talent.BLOOD_MIST);
	}

	public static float damageTaken( Hero hero ){
		return 1f - (0.2f + 0.05f*hero.pointsInTalent(Talent.COURAGE));
	}

	public static final int SPLIT_RADIUS = 5;

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Buff.affect(hero, Form.class).set(duration(hero));
		hero.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "name"));
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.8f);
		GameScene.flash(0x880000);
		CellEmitter.center(hero.pos).burst(BloodParticle.BURST, 12);

		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.RED_MIST;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.EGO_SENSITIVITY, Talent.BLOOD_MIST, Talent.COURAGE, Talent.HEROIC_ENERGY};
	}

	//the Red Mist herself: her E.G.O shows, and the great splits wait on the action button
	public static class Form extends Buff implements ActionIndicator.Action {

		{
			type = buffType.POSITIVE;
		}

		private int left;
		private int duration;
		private boolean usedVertical = false;
		private boolean usedHorizontal = false;

		public void set( int turns ){
			left = duration = turns;
			usedVertical = usedHorizontal = false;
			ActionIndicator.setAction(this);
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
		public void fx( boolean on ) {
			if (on){
				target.sprite.aura(0xDD1111, 5);
				if (!usedVertical || !usedHorizontal) ActionIndicator.setAction(this);
			} else {
				target.sprite.clearAura();
			}
		}

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.clearAction(this);
			//the subclass action takes the button back
			if (target instanceof Hero) GeburaTrackers.refreshActions((Hero) target);
		}

		@Override
		public int icon() {
			return BuffIndicator.RED_MIST;
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
			Hero hero = (Hero) target;
			return Messages.get(this, "desc",
					Math.round(damageBonus(hero)*1000)/10f,
					Math.round((1f - damageTaken(hero))*100),
					usedVertical ? Messages.get(this, "used") : Messages.get(this, "ready"),
					usedHorizontal ? Messages.get(this, "used") : Messages.get(this, "ready"),
					left);
		}

		@Override
		public String actionName() {
			return Messages.get(this, "action_name");
		}

		@Override
		public int actionIcon() {
			return HeroIcon.GREAT_SPLIT;
		}

		@Override
		public int indicatorColor() {
			return 0xAA0000;
		}

		@Override
		public void doAction() {
			GameScene.show(new WndOptions(new HeroIcon(this),
					Messages.titleCase(Messages.get(this, "action_name")),
					Messages.get(this, "choose"),
					Messages.get(this, "vertical"),
					Messages.get(this, "horizontal")){
				@Override
				protected boolean enabled( int index ) {
					return index == 0 ? !usedVertical : !usedHorizontal;
				}

				@Override
				protected void onSelect( int index ) {
					if (index == 0 && !usedVertical) aim(true);
					else if (index == 1 && !usedHorizontal) aim(false);
				}
			});
		}

		private void aim( boolean vertical ){
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect( Integer cell ) {
					if (cell == null) return;
					if (vertical) vertical(cell);
					else horizontal(cell);
				}

				@Override
				public String prompt() {
					return Messages.get(Form.class, vertical ? "prompt_vertical" : "prompt_horizontal");
				}
			});
		}

		private void afterSplit(){
			if (usedVertical && usedHorizontal) ActionIndicator.clearAction(this);
			BuffIndicator.refreshHero();
		}

		//a sure hit for 3x damage that paralyses for a turn
		private void vertical( int cell ){
			Hero hero = (Hero) target;
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !hero.canAttack(enemy)){
				GLog.w(Messages.get(Form.class, "no_target"));
				return;
			}
			usedVertical = true;
			hero.busy();
			hero.sprite.attack(cell, new Callback() {
				@Override
				public void call() {
					PixelScene.shake(2, 0.3f);
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
					if (hero.attack(enemy, 3f, 0f, Float.POSITIVE_INFINITY) && enemy.isAlive()){
						Buff.prolong(enemy, Paralysis.class, 1f);
					}
					CellEmitter.center(cell).burst(BloodParticle.BURST, 10);
					Invisibility.dispel();
					afterSplit();
					hero.spendAndNext(Actor.TICK);
				}
			});
		}

		//1.5x damage to every enemy in a half circle of radius 5 towards the chosen cell
		private void horizontal( int cell ){
			Hero hero = (Hero) target;
			if (cell == hero.pos){
				GLog.w(Messages.get(Form.class, "no_direction"));
				return;
			}
			usedHorizontal = true;

			int w = Dungeon.level.width();
			int hx = hero.pos % w, hy = hero.pos / w;
			int dx = cell % w - hx, dy = cell / w - hy;

			ArrayList<Integer> area = new ArrayList<>();
			for (int i = 0; i < Dungeon.level.length(); i++){
				int cx = i % w - hx, cy = i / w - hy;
				if (i == hero.pos || !Dungeon.level.heroFOV[i]) continue;
				if (cx*cx + cy*cy > SPLIT_RADIUS*SPLIT_RADIUS + SPLIT_RADIUS) continue;
				if (cx*dx + cy*dy < 0) continue;
				area.add(i);
			}

			ArrayList<Char> targets = new ArrayList<>();
			for (int i : area){
				Char ch = Actor.findChar(i);
				if (ch != null && ch != hero && ch.alignment != hero.alignment && !targets.contains(ch)){
					targets.add(ch);
				}
			}

			hero.busy();
			hero.sprite.attack(cell, new Callback() {
				@Override
				public void call() {
					PixelScene.shake(3, 0.4f);
					Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.7f);
					for (int i : area){
						CellEmitter.center(i).burst(BloodParticle.BURST, 2);
					}
					for (Char ch : targets){
						if (ch.isAlive()) hero.attack(ch, 1.5f, 0f, 1f);
					}
					Invisibility.dispel();
					afterSplit();
					hero.spendAndNext(Actor.TICK);
				}
			});
		}

		private static final String LEFT = "left";
		private static final String DURATION = "duration";
		private static final String VERTICAL = "used_vertical";
		private static final String HORIZONTAL = "used_horizontal";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(DURATION, duration);
			bundle.put(VERTICAL, usedVertical);
			bundle.put(HORIZONTAL, usedHorizontal);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			duration = bundle.getInt(DURATION);
			usedVertical = bundle.getBoolean(VERTICAL);
			usedHorizontal = bundle.getBoolean(HORIZONTAL);
		}
	}
}
