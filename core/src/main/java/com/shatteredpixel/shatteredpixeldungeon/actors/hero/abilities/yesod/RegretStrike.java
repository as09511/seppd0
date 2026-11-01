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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//Yesod's second armor ability: a crushing blow with +50% max damage that paralyzes the target
public class RegretStrike extends ArmorAbility {

	public static final float PARALYSIS = 5f;
	public static final float BOSS_PARALYSIS = 2f;

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	//pursuit: can reach enemies 2/3/4/5 tiles away by leaping next to them
	public static int reach( Hero hero ){
		return 1 + hero.pointsInTalent(Talent.PURSUIT);
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		if (target == null) return;

		final Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, target) > reach(hero)){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}

		int dest = hero.pos;
		if (!Dungeon.level.adjacent(hero.pos, target)){
			if (hero.rooted){
				PixelScene.shake( 1, 1f );
				return;
			}
			//the free cell next to the target that is closest to the hero
			dest = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int cell = target + n;
				if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null && Dungeon.level.heroFOV[cell]
						&& (dest == -1 || Dungeon.level.trueDistance(hero.pos, cell) < Dungeon.level.trueDistance(hero.pos, dest))){
					dest = cell;
				}
			}
			if (dest == -1){
				GLog.w(Messages.get(this, "blocked"));
				return;
			}
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();
		hero.busy();

		if (dest != hero.pos){
			final int landing = dest;
			float duration = 0.05f * Math.max(1, Dungeon.level.distance(hero.pos, landing));
			hero.sprite.jump(hero.pos, landing, 0, duration, new Callback() {
				@Override
				public void call() {
					hero.move(landing);
					Dungeon.level.occupyCell(hero);
					Dungeon.observe();
					GameScene.updateFog();
					strike(hero, enemy);
				}
			});
		} else {
			strike(hero, enemy);
		}
	}

	private void strike( Hero hero, Char enemy ){
		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				//metallic ring (ripple talent): the enemies around the target are found before the blow lands
				ArrayList<Char> around = new ArrayList<>();
				if (hero.hasTalent(Talent.RIPPLE)){
					for (int n : PathFinder.NEIGHBOURS8){
						Char ch = Actor.findChar(enemy.pos + n);
						if (ch != null && ch != hero && ch.alignment != hero.alignment){
							around.add(ch);
						}
					}
				}

				Buff.affect(hero, RegretTracker.class);
				hero.attack(enemy, 1f, 0, Char.INFINITE_ACCURACY);
				//each surrounding enemy takes its own regret hit (own damage roll and armor) at 15/30/45/60% damage
				for (Char ch : around){
					if (ch.isAlive()){
						hero.attack(ch, 0.15f * hero.pointsInTalent(Talent.RIPPLE), 0, Char.INFINITE_ACCURACY);
					}
				}
				Buff.detach(hero, RegretTracker.class);

				//the target and every surviving enemy hit by the ring are paralyzed
				paralyze(enemy);
				for (Char ch : around){
					paralyze(ch);
				}

				Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.8f);
				PixelScene.shake(2, 0.3f);
				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	private static void paralyze( Char ch ){
		if (ch.isAlive()){
			boolean boss = Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
			Buff.prolong(ch, Paralysis.class, boss ? BOSS_PARALYSIS : PARALYSIS);
		}
	}

	//while present, the hero's weapon hits with +50% max damage, and min correction raises the minimum
	public static class RegretTracker extends Buff {}

	//called from KindOfWeapon.damageRoll for the hero's weapon damage range
	public static int[] adjustRange( Hero hero, int min, int max ){
		if (hero.buff(RegretTracker.class) == null) return new int[]{min, max};
		max = Math.round(max * 1.5f);
		if (hero.hasTalent(Talent.MIN_CORRECTION)){
			//minimum is at least 30/45/60/75% of the maximum
			min = Math.max(min, Math.round(max * (0.15f + 0.15f*hero.pointsInTalent(Talent.MIN_CORRECTION))));
		}
		return new int[]{Math.min(min, max), max};
	}

	@Override
	public int icon() {
		return HeroIcon.SHOCKWAVE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.MIN_CORRECTION, Talent.RIPPLE, Talent.PURSUIT, Talent.HEROIC_ENERGY};
	}
}
