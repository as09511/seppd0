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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FallingStar;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Netzach's second armor ability: our galaxy. Blue starlight falls on every visible enemy nearby for 3-6 damage
//that ignores armor, and Netzach heals for all the damage dealt.
public class OurGalaxy extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	//7x7, starlight: 9x9 / 11x11 / 13x13
	public static int radius( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.STARLIGHT);
	}

	//starfall: +1 to both ends per point
	public static int minDamage( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.STARFALL);
	}

	public static int maxDamage( Hero hero ){
		return 6 + hero.pointsInTalent(Talent.STARFALL);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = radius(hero);
		ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.heroFOV[m.pos]
					&& Dungeon.level.distance(hero.pos, m.pos) <= r){
				targets.add(m);
			}
		}

		hero.busy();
		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		Invisibility.dispel();

		if (targets.isEmpty()){
			finish(hero, 0);
			return;
		}

		//blue starlight falls from the sky onto each enemy, the damage lands with it
		final int[] remaining = {targets.size()};
		final int[] dealt = {0};
		for (Char ch : targets){
			hero.sprite.parent.add(new FallingStar(ch.sprite.center(), () -> {
				if (ch.isAlive()){
					int before = ch.HP + ch.shielding();
					//fixed damage: the damage dealt bonuses don't apply
					ch.damage(Random.NormalIntRange(minDamage(hero), maxDamage(hero)), hero);
					dealt[0] += Math.max(0, before - (ch.HP + ch.shielding()));
					if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
				}
				if (--remaining[0] == 0){
					finish(hero, dealt[0]);
				}
			}));
		}
	}

	//the stolen starlight heals Netzach, galaxy embrace turns 33/66/100/133% of the excess into a shield
	private void finish( Hero hero, int dealt ){
		if (dealt > 0){
			int heal = Math.min(dealt, hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			int excess = dealt - heal;
			if (excess > 0 && hero.hasTalent(Talent.GALAXY_EMBRACE)){
				int shield = Math.round(excess * hero.pointsInTalent(Talent.GALAXY_EMBRACE) / 3f);
				if (shield > 0){
					Buff.affect(hero, Barrier.class).incShield(shield);
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
				}
			}
		}
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.OUR_GALAXY;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.STARLIGHT, Talent.STARFALL, Talent.GALAXY_EMBRACE, Talent.HEROIC_ENERGY};
	}
}
