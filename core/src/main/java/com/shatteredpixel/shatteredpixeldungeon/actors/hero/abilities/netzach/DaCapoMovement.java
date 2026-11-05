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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.netzach;

import com.shatteredpixel.shatteredpixeldungeon.lobotomy.HeroDamageBonus;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.DaCapoScore;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Netzach's third armor ability: da capo, a performance in five movements.
//Each use deals WHITE damage to every enemy around him and debuffs them, walls and doors don't stop the music. Used
//again on the very next turn the next movement plays, wider and stronger, with another score ring around him; the
//rings stay while the performance goes on, any other action ends it:
// 1st 5x5 12-24 weakness, 2nd 7x7 blindness, 3rd 9x9 vertigo, 4th 11x11 vulnerable,
// finale 13x13 amok (2 turns of paralysis on bosses), 5 turns each. In the finale the score spreads out and strikes.
public class DaCapoMovement extends ArmorAbility {

	{
		baseChargeUse = 20f;
	}

	public static final int MOVEMENTS = 5;

	//the next movement has to be played on the very next turn: the tracker outlives the 1 turn the ability takes
	// by half a turn, so any other action lets it run out
	private static final float WINDOW = 1.5f;

	//5x5, 7x7, 9x9, 11x11, then 13x13 for the finale
	public static int radius( int movement ){
		return movement + 1;
	}

	//trance: 10/20/30/40% less damage taken while the performance goes on
	public static float damageTakenFactor( Hero hero ){
		if (hero.buff(Movement.class) == null || !hero.hasTalent(Talent.TRANCE)) return 1f;
		return 1f - 0.1f*hero.pointsInTalent(Talent.TRANCE);
	}

	//12-24, +4-8 per movement. fortissimo: +10/20/30%
	public static int minDamage( Hero hero, int movement ){
		return Math.round((12 + 4*(movement-1)) * (1f + 0.1f*hero.pointsInTalent(Talent.FORTISSIMO)));
	}

	public static int maxDamage( Hero hero, int movement ){
		return Math.round((24 + 8*(movement-1)) * (1f + 0.1f*hero.pointsInTalent(Talent.FORTISSIMO)));
	}

	//5 turns, fermata: +1 per point
	public static float debuffDuration( Hero hero ){
		return 5f + hero.pointsInTalent(Talent.FERMATA);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Movement tracker = hero.buff(Movement.class);
		int movement = tracker == null ? 1 : tracker.next;

		int r = radius(movement);
		ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive()
					&& Dungeon.level.distance(hero.pos, m.pos) <= r){
				targets.add(m);
			}
		}

		Sample.INSTANCE.play(Assets.Sounds.BEACON, 1f, 0.8f + 0.1f*movement);
		hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "movement_" + movement));
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();

		//the finale: the score built up so far spreads out, striking each enemy as it reaches them
		if (movement >= MOVEMENTS){
			DaCapoScore score = tracker != null ? tracker.takeScore() : null;
			Buff.detach(hero, Movement.class);
			if (score == null || score.parent == null){
				if (score != null) score.killAndErase();
				score = new DaCapoScore(hero.sprite, DaCapoScore.MAX_RINGS);
				hero.sprite.parent.add(score);
			}
			hero.busy();
			PixelScene.shake(3, 0.6f);
			score.spread(targets, ch -> strike(hero, ch, movement), () -> hero.spendAndNext(1f));
			return;
		}

		for (Char ch : targets){
			strike(hero, ch, movement);
		}

		if (tracker == null){
			//attaching the tracker puts up the first ring of the score
			tracker = Buff.prolong(hero, Movement.class, WINDOW);
			tracker.next = 2;
		} else {
			Buff.prolong(hero, Movement.class, WINDOW);
			tracker.next = movement + 1;
			tracker.addRing();
		}

		hero.spendAndNext(1f);
	}

	private static void strike( Hero hero, Char ch, int movement ){
		if (!ch.isAlive()) return;
		int dmg = HeroDamageBonus.direct(hero, ch, Random.NormalIntRange(minDamage(hero, movement), maxDamage(hero, movement)));
		dmg = DamageType.apply(ch, DamageType.WHITE, dmg);
		if (dmg > 0) ch.damage(dmg, hero);
		ch.incomingDamageType = null;
		if (!ch.isAlive()) return;

		float duration = debuffDuration(hero);
		switch (movement){
			case 1: default: Buff.prolong(ch, Weakness.class, duration); break;
			case 2: Buff.prolong(ch, Blindness.class, duration); break;
			case 3: Buff.prolong(ch, Vertigo.class, duration); break;
			case 4: Buff.prolong(ch, Vulnerable.class, duration); break;
			case 5:
				if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)){
					Buff.prolong(ch, Paralysis.class, 2f);
				} else {
					Buff.prolong(ch, Amok.class, duration);
				}
				break;
		}
	}

	@Override
	public int icon() {
		return HeroIcon.DA_CAPO;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.TRANCE, Talent.FORTISSIMO, Talent.FERMATA, Talent.HEROIC_ENERGY};
	}

	//which movement plays next, for as long as the performance can still go on
	public static class Movement extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public int next = 2;

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xB070E0);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(next);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Messages.get(DaCapoMovement.class, "movement_" + next));
		}

		//the score stays around Netzach for as long as the performance goes on, one ring per movement played
		private DaCapoScore score;

		@Override
		public void fx( boolean on ) {
			if (on){
				if (target.sprite == null || target.sprite.parent == null) return;
				if (score != null && score.alive && score.parent == target.sprite.parent
						&& score.target() == target.sprite) return;
				score = new DaCapoScore(target.sprite, Math.max(1, Math.min(DaCapoScore.MAX_RINGS, next - 1)));
				target.sprite.parent.add(score);
			} else if (score != null){
				score.fade();
				score = null;
			}
		}

		public void addRing(){
			if (score == null) fx(true);
			else if (score.rings() < next - 1) score.addRing();
		}

		//the finale takes the score over, so it isn't faded out when the tracker goes
		public DaCapoScore takeScore(){
			DaCapoScore s = score;
			score = null;
			return s;
		}

		private static final String NEXT = "next";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(NEXT, next);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			next = bundle.getInt(NEXT);
		}
	}
}
