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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.AbilityCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.HeroDamageBonus;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Tiphereth's third armor ability: the area around her goes dark for a moment, every enemy in it takes 6-16 damage,
//and if her weapon needs 10 strength or less they are also weakened, made vulnerable, rooted and paralysed for 4 turns
public class Nihil extends ArmorAbility {

	//a 7x7 square to begin with
	public static final int BASE_RANGE = 3;
	public static final int DEBUFF_TURNS = 4;
	public static final int LIGHT_WEAPON_STR = 10;

	{
		baseChargeUse = 30f;
	}

	//deep void: every 2 points widen the square by a tile on each side (9x9, 11x11),
	//an odd point already reaches the next ring, all but its corners
	public static boolean inArea( Hero hero, int cell ){
		int pts = hero.pointsInTalent(Talent.DEEP_VOID);
		int range = BASE_RANGE + pts/2;
		int w = Dungeon.level.width();
		int dx = Math.abs(cell % w - hero.pos % w);
		int dy = Math.abs(cell / w - hero.pos / w);
		if (dx <= range && dy <= range) return true;
		return pts % 2 == 1 && dx <= range+1 && dy <= range+1 && !(dx == range+1 && dy == range+1);
	}

	//deception: +50% damage per point
	public static int damageRoll( Hero hero ){
		return Math.round(Random.NormalIntRange(6, 16) * (1f + 0.5f*hero.pointsInTalent(Talent.DECEPTION)));
	}

	//echo of nihil: the debuffs last 1 more turn per point
	public static float debuffTurns( Hero hero ){
		return DEBUFF_TURNS + hero.pointsInTalent(Talent.VOID_ECHO);
	}

	//her weapon finally needs 10 strength or less, after upgrades, the potion of mastery and proof of competence
	//(fists and the worn ribbon count by their own requirement)
	public static boolean lightWeapon( Hero hero ){
		KindOfWeapon w = hero.belongings.weapon();
		return w == null || (w instanceof Weapon && ((Weapon) w).STRReq() <= LIGHT_WEAPON_STR);
	}

	@Override
	protected void activate( AbilityCrown armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		boolean light = lightWeapon(hero);

		//the area blacks out for a moment
		for (int cell = 0; cell < Dungeon.level.length(); cell++){
			if (inArea(hero, cell) && Dungeon.level.heroFOV[cell]){
				ColorBlock dark = new ColorBlock(DungeonTilemap.SIZE, DungeonTilemap.SIZE, 0xFF000000);
				PointF p = DungeonTilemap.tileToWorld(cell);
				dark.x = p.x;
				dark.y = p.y;
				GameScene.effect(dark);
				dark.parent.add(new AlphaTweener(dark, 0f, 0.8f){
					@Override
					protected void onComplete() {
						super.onComplete();
						dark.killAndErase();
					}
				});
			}
		}
		PixelScene.shake(2, 0.3f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 0.6f);

		ArrayList<Char> targets = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && inArea(hero, m.pos)){
				targets.add(m);
			}
		}
		float turns = debuffTurns(hero);
		for (Char ch : targets){
			ch.damage(HeroDamageBonus.direct(hero, ch, damageRoll(hero)), hero);
			if (ch.isAlive() && light){
				Buff.prolong(ch, Weakness.class, turns);
				Buff.prolong(ch, Vulnerable.class, turns);
				Buff.prolong(ch, Roots.class, turns);
				Buff.prolong(ch, Paralysis.class, turns);
			}
		}

		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.SHOCKWAVE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.DEEP_VOID, Talent.DECEPTION, Talent.VOID_ECHO, Talent.HEROIC_ENERGY};
	}
}
