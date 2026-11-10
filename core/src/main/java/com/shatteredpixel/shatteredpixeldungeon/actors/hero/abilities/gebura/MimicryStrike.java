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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.HeroDamageBonus;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//Gebura's third armor ability: Mimicry tears into an enemy next to her for 19-35 RED damage and 10 bleeding,
// and she heals for the damage dealt.
//Good Bye: +25/50/75/100% damage. Hello?: 33/66/100/133% of the healing past her max health becomes shielding.
//Husk: immune to effects that stop her acting for 6/9/12/15 turns.
public class MimicryStrike extends ArmorAbility {

	public static final int BLEED = 10;

	{
		baseChargeUse = 50f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return dst;
	}

	public static int damageRoll( Hero hero ){
		float dmg = Random.NormalIntRange(19, 35);
		dmg *= 1f + 0.25f*hero.pointsInTalent(Talent.GOOD_BYE);
		return Math.round(dmg);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment
				|| !Dungeon.level.adjacent(hero.pos, target)){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		hero.busy();
		hero.sprite.attack(target, new Callback() {
			@Override
			public void call() {
				strike(hero, enemy);
				Invisibility.dispel();
				hero.spendAndNext(Actor.TICK);
			}
		});
	}

	private void strike( Hero hero, Char enemy ){
		Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.8f);
		CellEmitter.center(enemy.pos).burst(BloodParticle.BURST, 10);

		int dealt = 0;
		if (enemy.isAlive()){
			int before = enemy.HP + enemy.shielding();
			int dmg = HeroDamageBonus.direct(hero, enemy, damageRoll(hero), enemy.drRoll());
			dmg = DamageType.apply(enemy, DamageType.RED, dmg);
			enemy.damage(dmg, hero);
			enemy.incomingDamageType = null;
			dealt = Math.max(0, before - Math.max(0, enemy.HP + enemy.shielding()));
			if (enemy.isAlive()){
				Buff.affect(enemy, Bleeding.class).set(BLEED);
			}
		}

		//the healing, any of it past max health turns into shielding with Hello?
		if (dealt > 0){
			int heal = Math.min(dealt, hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			int over = dealt - heal;
			int shield = Math.round(over * hero.pointsInTalent(Talent.HELLO) / 3f);
			if (shield > 0){
				Buff.affect(hero, Barrier.class).incShield(shield);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}
		}

		//Husk: 6/9/12/15 turns immune to effects that stop her acting
		if (hero.hasTalent(Talent.HUSK)){
			Buff.prolong(hero, Husk.class, 3 + 3*hero.pointsInTalent(Talent.HUSK));
		}
	}

	@Override
	public int icon() {
		return HeroIcon.MIMICRY_STRIKE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.GOOD_BYE, Talent.HELLO, Talent.HUSK, Talent.HEROIC_ENERGY};
	}

	public static class Husk extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			immunities.add(Paralysis.class);
			immunities.add(Frost.class);
			immunities.add(MagicalSleep.class);
			immunities.add(Drowsy.class);
			immunities.add(Terror.class);
			immunities.add(Dread.class);
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				for (Class<?> c : immunities){
					for (Buff b : target.buffs()){
						if (c.isInstance(b)) b.detach();
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public int icon() {
			return BuffIndicator.IMMUNITY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.8f, 0.2f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (15f - visualcooldown()) / 15f);
		}
	}
}
