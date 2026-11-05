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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.tiphereth;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Tiphereth's first armor ability: for a few turns her shielding is refilled every turn.
//Shield spread hands part of it to the allies in sight, Tiphereth B included.
public class ShieldBarrage extends ArmorAbility {

	public static final int BASE_TURNS = 5;
	public static final float BASE_SHIELD = 0.2f;

	{
		baseChargeUse = 50f;
	}

	//lasting shield: +1 turn per point
	public static int turns( Hero hero ){
		return BASE_TURNS + hero.pointsInTalent(Talent.LASTING_SHIELD);
	}

	//20% of her max health, shield output: +12.5% of that per point
	public static int shield( Hero hero ){
		return Math.max(1, Math.round(hero.HT * BASE_SHIELD * (1f + 0.125f*hero.pointsInTalent(Talent.SHIELD_OUTPUT))));
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int turns = turns(hero);
		int amount = shield(hero);
		Buff.affect(hero, Barrage.class).set(amount, turns);
		new Flare(6, 32).color(0xFFE070, true).show(hero.sprite, 1.5f);

		//shield spread: allies in sight get 25/50/75/100% of it
		if (hero.hasTalent(Talent.SHIELD_SPREAD)){
			int shared = Math.round(amount * 0.25f * hero.pointsInTalent(Talent.SHIELD_SPREAD));
			if (shared > 0){
				for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
					if (m.alignment == Char.Alignment.ALLY && m.isAlive() && Dungeon.level.heroFOV[m.pos]){
						Buff.affect(m, Barrage.class).set(shared, turns);
						if (m.sprite != null) new Flare(5, 20).color(0xFFE070, true).show(m.sprite, 1f);
					}
				}
			}
		}

		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.2f);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	//shielding that is filled back up to its amount at the start of every turn, until it runs out of turns
	public static class Barrage extends ShieldBuff {

		{
			type = buffType.POSITIVE;
			detachesAtZero = false;
		}

		private int amount;
		private int left;

		public void set( int amount, int turns ){
			this.amount = Math.max(this.amount, amount);
			this.left = Math.max(this.left, turns);
			setShield(this.amount);
			if (target.sprite != null) target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(this.amount), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING);
		}

		@Override
		public boolean act() {
			left--;
			if (left <= 0){
				detach();
				return true;
			}
			if (shielding() < amount) setShield(amount);
			spend(TICK);
			return true;
		}

		@Override
		public void fx( boolean on ) {
			if (on) target.sprite.add(CharSprite.State.SHIELDED);
			else target.sprite.remove(CharSprite.State.SHIELDED);
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.9f, 0.3f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", amount, left);
		}

		private static final String AMOUNT = "amount";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(AMOUNT, amount);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			amount = bundle.getInt(AMOUNT);
			left = bundle.getInt(LEFT);
		}
	}

	@Override
	public int icon() {
		return HeroIcon.ENDURE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.LASTING_SHIELD, Talent.SHIELD_OUTPUT, Talent.SHIELD_SPREAD, Talent.HEROIC_ENERGY};
	}
}
