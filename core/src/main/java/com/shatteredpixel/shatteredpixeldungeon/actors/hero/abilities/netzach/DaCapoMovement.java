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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.DaCapoScore;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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

//Netzach's third armor ability: da capo, a performance in four movements.
//Each use damages and debuffs every visible enemy around him; used again soon after, the next movement plays,
//wider and stronger, with another score ring around him:
// 1st 5x5 12-24 weakness, 2nd 7x7 cripple, 3rd 9x9 vulnerable, 4th 9x9 paralysis (2 turns on bosses), 5 turns each
public class DaCapoMovement extends ArmorAbility {

	{
		baseChargeUse = 25f;
	}

	public static final int MOVEMENTS = 4;

	//the next movement has to be played within 10 turns, encore: 15/20/25
	public static float window( Hero hero ){
		return 10f + 5f*hero.pointsInTalent(Talent.ENCORE);
	}

	public static int radius( int movement ){
		return Math.min(4, movement + 1);
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
		float duration = debuffDuration(hero);
		ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.heroFOV[m.pos]
					&& Dungeon.level.distance(hero.pos, m.pos) <= r){
				targets.add(m);
			}
		}
		for (Char ch : targets){
			ch.damage(Random.NormalIntRange(minDamage(hero, movement), maxDamage(hero, movement)), hero);
			if (!ch.isAlive()) continue;
			switch (movement){
				case 1: default: Buff.prolong(ch, Weakness.class, duration); break;
				case 2: Buff.prolong(ch, Cripple.class, duration); break;
				case 3: Buff.prolong(ch, Vulnerable.class, duration); break;
				case 4:
					float paralysis = Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)
							? 2f : duration;
					Buff.prolong(ch, Paralysis.class, paralysis);
					break;
			}
		}

		hero.sprite.parent.add(new DaCapoScore(hero.sprite, movement));
		if (movement >= MOVEMENTS) PixelScene.shake(3, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.BEACON, 1f, 0.8f + 0.1f*movement);
		hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "movement_" + movement));

		//after the last movement the performance starts over
		if (movement >= MOVEMENTS){
			Buff.detach(hero, Movement.class);
		} else {
			Buff.affect(hero, Movement.class, window(hero)).next = movement + 1;
		}

		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.DA_CAPO;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.ENCORE, Talent.FORTISSIMO, Talent.FERMATA, Talent.HEROIC_ENERGY};
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
			return Messages.get(this, "desc", next, dispTurns());
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
