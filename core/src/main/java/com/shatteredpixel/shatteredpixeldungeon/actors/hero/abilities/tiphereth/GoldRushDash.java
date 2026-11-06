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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.GoldRush;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.HeroDamageBonus;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Tiphereth's second armor ability: she opens the golden road (a golden circle on her as she leaves and arrives),
//steps through it to the enemy's side, strikes for 5-15 RED damage and gains 5 stacks of the golden road for 2 turns
public class GoldRushDash extends ArmorAbility {

	public static final int BASE_RANGE = 4;
	public static final int STACKS = 5;
	public static final float CHAIN_TURNS = 3f;

	{
		baseChargeUse = 25f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	//golden dash: +1 range per point
	public static int range( Hero hero ){
		return BASE_RANGE + hero.pointsInTalent(Talent.GOLDEN_DASH);
	}

	public static int damageRoll( Hero hero ){
		return Random.NormalIntRange(5, 15);
	}

	//golden chain: used again within 3 turns, it costs 20/30/40/50% less charge
	@Override
	public float chargeUse( Hero hero ) {
		float use = super.chargeUse(hero);
		int pts = hero.pointsInTalent(Talent.GOLDEN_STRIKE);
		if (pts > 0 && hero.buff(Chain.class) != null){
			use *= 1f - (0.1f + 0.1f*pts);
		}
		return use;
	}

	//endless road: the golden road lasts 1 more turn per point
	public static int roadTurns( Hero hero ){
		return GoldRush.GoldenRoad.DURATION + hero.pointsInTalent(Talent.ENDLESS_ROAD);
	}

	//the window in which golden chain makes the next gold rush cheaper
	public static class Chain extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		if (target == null) return;
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, target) > range(hero)){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}
		if (hero.rooted){
			PixelScene.shake(1, 1f);
			return;
		}

		//the cell next to the enemy closest to Tiphereth, already next to it counts too
		int dest = -1;
		if (Dungeon.level.adjacent(hero.pos, target)){
			dest = hero.pos;
		} else {
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target + n;
				if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				if (dest == -1 || Dungeon.level.trueDistance(c, hero.pos) < Dungeon.level.trueDistance(dest, hero.pos)){
					dest = c;
				}
			}
		}
		if (dest == -1){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();
		if (hero.hasTalent(Talent.GOLDEN_STRIKE)){
			Buff.prolong(hero, Chain.class, CHAIN_TURNS);
		}

		//a golden circle opens on Tiphereth as she leaves
		circle(hero);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

		hero.busy();
		final int to = dest;
		hero.sprite.parent.add(new com.watabou.noosa.tweeners.Delayer(0.25f){
			@Override
			protected void onComplete() {
				if (to != hero.pos){
					ScrollOfTeleportation.appear(hero, to);
					//and again where she comes out
					circle(hero);
					Dungeon.level.occupyCell(hero);
					Dungeon.observe();
					GameScene.updateFog();
				}
				hero.sprite.attack(enemy.pos, new Callback() {
					@Override
					public void call() {
						strike(hero, enemy);
						Invisibility.dispel();
						hero.spendAndNext(Actor.TICK);
					}
				});
			}
		});
	}

	private static void circle( Char ch ){
		new Flare(8, 20).color(0xFFD040, true).show(ch.sprite, 1f);
	}

	private void strike( Hero hero, Char enemy ){
		if (enemy.isAlive()){
			int dmg = HeroDamageBonus.direct(hero, enemy, damageRoll(hero), enemy.drRoll());
			dmg = DamageType.apply(enemy, DamageType.RED, dmg);
			enemy.damage(dmg, hero);
			enemy.incomingDamageType = null;
		}
		//the golden hit
		new Flare(6, 28).color(0xFFE070, true).show(enemy.sprite, 0.6f);
		CellEmitter.center(enemy.pos).burst(Speck.factory(Speck.YELLOW_LIGHT), 8);
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 1.1f);
		PixelScene.shake(1, 0.2f);

		Buff.affect(hero, GoldRush.GoldenRoad.class).surge(STACKS, roadTurns(hero));
	}

	@Override
	public int icon() {
		return HeroIcon.GOLD_RUSH;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.GOLDEN_DASH, Talent.GOLDEN_STRIKE, Talent.ENDLESS_ROAD, Talent.HEROIC_ENERGY};
	}
}
