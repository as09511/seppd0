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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//testing tool, summons a training dummy (or a quest npc) next to the hero
public class DummySummoner extends Item {

	public static final String AC_SUMMON = "SUMMON";

	@SuppressWarnings("unchecked")
	private static final Class<? extends Mob>[] DUMMIES = new Class[]{
			TrainingDummy.Resistant.class,
			TrainingDummy.Weak.class,
			TrainingDummy.PaleWeak.class,
			TrainingDummy.Ally.class,
			Angela.class
	};

	{
		image = ItemSpriteSheet.MASTERY;
		defaultAction = AC_SUMMON;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SUMMON);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_SUMMON)){
			String[] names = new String[DUMMIES.length];
			for (int i = 0; i < DUMMIES.length; i++){
				names[i] = Messages.titleCase(Reflection.newInstance(DUMMIES[i]).name());
			}

			GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()), Messages.get(this, "choose"), names){
				@Override
				protected void onSelect(int index) {
					ArrayList<Integer> cells = new ArrayList<>();
					for (int n : PathFinder.NEIGHBOURS8){
						int cell = hero.pos + n;
						if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null){
							cells.add(cell);
						}
					}
					if (cells.isEmpty()){
						GLog.w(Messages.get(DummySummoner.class, "no_space"));
						return;
					}

					Mob dummy = Reflection.newInstance(DUMMIES[index]);
					GameScene.add(dummy);
					ScrollOfTeleportation.appear(dummy, Random.element(cells));
				}
			});
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

}
