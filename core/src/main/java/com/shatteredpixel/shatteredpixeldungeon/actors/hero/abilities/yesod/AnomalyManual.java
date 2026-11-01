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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.yesod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.InfoPower;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Yesod's first armor ability: following the abnormality response manual halves damage taken for a while
public class AnomalyManual extends ArmorAbility {

	public static final float BASE_DURATION = 5f;

	{
		baseChargeUse = 50f;
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ManualBuff buff = Buff.prolong(hero, ManualBuff.class, BASE_DURATION + hero.pointsInTalent(Talent.EXTENDED_MANUAL));
		buff.recordsLeft = hero.pointsInTalent(Talent.DAMAGE_RECORD);

		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.FORGE), 8);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		hero.spendAndNext(1f);
	}

	//called from Hero.damage, returns the damage left after the manual's 50% reduction
	public static float onDamage( Hero hero, float damage, Object src ){
		ManualBuff buff = hero.buff(ManualBuff.class);
		if (buff == null || damage <= 0) return damage;
		float reduced = damage * 0.5f;
		damage -= reduced;
		if (src instanceof Char){
			//damage record: +1 info per hit, up to 1/2/3/4 times per use
			if (buff.recordsLeft > 0){
				buff.recordsLeft--;
				InfoPower.gain(1);
			}
			//counter guideline: 25/50/75/100% of the prevented damage is returned
			if (hero.hasTalent(Talent.COUNTER_GUIDELINE)){
				int reflect = Math.round(reduced * 0.25f * hero.pointsInTalent(Talent.COUNTER_GUIDELINE));
				Char attacker = (Char) src;
				if (reflect > 0 && attacker.isAlive() && attacker != hero){
					attacker.damage(reflect, hero);
				}
			}
		}
		return damage;
	}

	public static class ManualBuff extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int recordsLeft = 0;

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.5f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (BASE_DURATION - visualcooldown()) / BASE_DURATION);
		}

		private static final String RECORDS = "records_left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(RECORDS, recordsLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			recordsLeft = bundle.getInt(RECORDS);
		}
	}

	@Override
	public int icon() {
		return HeroIcon.ANOMALY_MANUAL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.EXTENDED_MANUAL, Talent.DAMAGE_RECORD, Talent.COUNTER_GUIDELINE, Talent.HEROIC_ENERGY};
	}
}
