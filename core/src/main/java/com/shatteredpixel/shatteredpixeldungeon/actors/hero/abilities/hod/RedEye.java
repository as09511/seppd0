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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//Hod's third armor ability: drags an enemy within 4 tiles in front of her and strikes it,
// rooting, weakening and exposing it. If it can't be dragged, she leaps to it instead.
public class RedEye extends ArmorAbility {

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

	//long gaze: 4 tiles, 5/6/7/8 with the talent
	public static int range( Hero hero ){
		return 4 + hero.pointsInTalent(Talent.LONG_GAZE);
	}

	//vigilance: the debuffs last 3 turns, +1 per point
	public static float debuffDuration( Hero hero ){
		return 3f + hero.pointsInTalent(Talent.VIGILANCE);
	}

	@Override
	protected void activate(AbilityCrown armor, Hero hero, Integer target) {
		if (target == null) return;

		final Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || enemy.alignment == hero.alignment || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, target) > range(hero)){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}

		if (Dungeon.level.adjacent(hero.pos, enemy.pos)){
			armor.charge -= chargeUse(hero);
			Item.updateQuickslot();
			hero.busy();
			strike(hero, enemy);
			return;
		}

		int pullCell = pullCell(hero, enemy);
		if (pullCell != -1){
			armor.charge -= chargeUse(hero);
			Item.updateQuickslot();
			hero.busy();
			final int from = enemy.pos;
			float duration = 0.05f * Math.max(1, Dungeon.level.distance(from, pullCell));
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			enemy.sprite.jump(from, pullCell, 0, duration, new Callback() {
				@Override
				public void call() {
					enemy.pos = pullCell;
					Dungeon.level.occupyCell(enemy);
					Dungeon.observe();
					GameScene.updateFog();
					strike(hero, enemy);
				}
			});
			return;
		}

		//can't be dragged: leap to the free cell next to it that is closest to the hero
		if (hero.rooted){
			PixelScene.shake( 1, 1f );
			return;
		}
		int dest = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int cell = enemy.pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null && Dungeon.level.heroFOV[cell]
					&& (dest == -1 || Dungeon.level.trueDistance(hero.pos, cell) < Dungeon.level.trueDistance(hero.pos, dest))){
				dest = cell;
			}
		}
		if (dest == -1){
			GLog.w(Messages.get(this, "blocked"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();
		hero.busy();
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
	}

	//the cell right in front of the hero on the line to the enemy, or -1 if the enemy can't be dragged there
	private static int pullCell( Hero hero, Char enemy ){
		if (Char.hasProp(enemy, Char.Property.IMMOVABLE) || enemy.rooted) return -1;
		Ballistica line = new Ballistica(hero.pos, enemy.pos, Ballistica.PROJECTILE);
		if (line.collisionPos != enemy.pos || line.path.size() < 2) return -1;
		int cell = line.path.get(1);
		if (!Dungeon.level.passable[cell] || Actor.findChar(cell) != null) return -1;
		if (Dungeon.level.pit[cell] && !enemy.flying) return -1;
		if (Char.hasProp(enemy, Char.Property.LARGE) && !Dungeon.level.openSpace[cell]) return -1;
		return cell;
	}

	private static void strike( Hero hero, Char enemy ){
		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				int before = enemy.HP + enemy.shielding();
				hero.attack(enemy, 1f, 0, 1f);
				int dealt = before - (enemy.HP + enemy.shielding());

				//feast: heals 7.5/15/22.5/30% of the damage dealt
				if (dealt > 0 && hero.hasTalent(Talent.FEAST)){
					int heal = Math.min(Math.round(dealt * 0.075f * hero.pointsInTalent(Talent.FEAST)), hero.HT - hero.HP);
					if (heal > 0){
						hero.HP += heal;
						hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
				}

				if (enemy.isAlive()){
					float duration = debuffDuration(hero);
					Buff.prolong(enemy, Roots.class, duration);
					Buff.prolong(enemy, Weakness.class, duration);
					Buff.prolong(enemy, Vulnerable.class, duration);
				}

				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
			}
		});
	}

	@Override
	public int icon() {
		return HeroIcon.SPIRIT_HAWK;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.LONG_GAZE, Talent.VIGILANCE, Talent.FEAST, Talent.HEROIC_ENERGY};
	}
}
