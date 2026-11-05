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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.hod;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollExile;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//Hod's second armor ability: a strike that turns half the damage dealt into bleeding,
// then opens a deep wound that keeps the bleeding from going down
public class BloodyDesire extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	//axe: 50% of the damage as bleeding, 60/70/80/90% with the talent
	public static float bleedRatio( Hero hero ){
		return 0.5f + 0.1f*hero.pointsInTalent(Talent.AXE);
	}

	//slash: the deep wound lasts 1 turn, +1 per point
	public static float woundDuration( Hero hero ){
		return 1f + hero.pointsInTalent(Talent.SLASH);
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		if (target == null) return;

		final Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (!hero.canAttack(enemy)){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();
		hero.busy();

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				int before = enemy.HP + enemy.shielding();
				hero.attack(enemy, 1f, 0, 1f);
				int dealt = before - (enemy.HP + enemy.shielding());

				if (enemy.isAlive() && dealt > 0){
					float bleed = dealt * bleedRatio(hero);
					if (bleed >= 1f){
						Bleeding b = Buff.affect(enemy, Bleeding.class);
						if (b.level() < bleed) b.extend(bleed - b.level());
					}
					Buff.prolong(enemy, DeepWound.class, woundDuration(hero));
				}

				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.8f);
				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	//sparkle: when an enemy with a deep wound dies, 25/50/75/100% of its bleeding spreads to the enemies around it
	public static void onDeath( Char ch ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.hasTalent(Talent.SPARKLE) || ch.buff(DeepWound.class) == null) return;
		Bleeding b = ch.buff(Bleeding.class);
		if (b == null || b.level() < 1f) return;
		float spread = b.level() * 0.25f * hero.pointsInTalent(Talent.SPARKLE);
		if (spread < 1f) return;
		for (int n : PathFinder.NEIGHBOURS8){
			Char other = Actor.findChar(ch.pos + n);
			if (other != null && other.isAlive() && other != hero && other.alignment != hero.alignment
					&& !GnollExile.peaceful(other)){
				Bleeding ob = Buff.affect(other, Bleeding.class);
				if (ob.level() < spread) ob.extend(spread - ob.level());
			}
		}
	}

	//while a deep wound is open, bleeding does not go down
	public static class DeepWound extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.BLEEDING;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.5f, 0f, 0.1f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	@Override
	public int icon() {
		return HeroIcon.BLOODY_DESIRE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SLASH, Talent.AXE, Talent.SPARKLE, Talent.HEROIC_ENERGY};
	}
}
