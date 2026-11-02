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


package com.shatteredpixel.shatteredpixeldungeon.items.armor.ego;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//ALEPH, same defense as the tier 5 armor
//its wearer can eat the piles of bones found in the dungeon to heal, by examining one next to them
public class LaughterArmor extends EgoArmor {

	public static final int HEAL = 10;

	{
		image = ItemSpriteSheet.LAUGHTER_ARMOR;

		risk = RiskLevel.ALEPH;
		damageMultipliers = new float[]{0.5f, 0.5f, 0.2f, 1f};
	}

	public LaughterArmor() {
		super( 5 );
	}

	//called when the hero examines a cell, returns true if she ate the bones there
	//the pile is opened as if searched, so whatever was in it stays behind
	public static boolean tryEat( Hero hero, int cell ){
		if (hero == null || !(hero.belongings.armor() instanceof LaughterArmor)) return false;
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null || !heap.seen || (heap.type != Heap.Type.SKELETON && heap.type != Heap.Type.REMAINS)) return false;
		if (cell != hero.pos && !Dungeon.level.adjacent(cell, hero.pos)){
			GLog.w(Messages.get(LaughterArmor.class, "too_far"));
			return false;
		}

		hero.sprite.operate(cell);
		heap.open(hero);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		GLog.i(Messages.get(LaughterArmor.class, "eat"));
		int heal = Math.min(HEAL, hero.HT - hero.HP);
		if (heal > 0 && hero.isAlive()){
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsInfo() {
		return super.statsInfo() + "\n\n" + Messages.get(this, "stats_desc", HEAL);
	}

}
