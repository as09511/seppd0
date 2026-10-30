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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//testing tool, creates any E.G.O equipment: identified, uncursed, unenchanted and +0
public class EgoExtractor extends Item {

	public static final String AC_EXTRACT = "EXTRACT";

	{
		image = ItemSpriteSheet.CRYSTAL_CHEST;
		defaultAction = AC_EXTRACT;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_EXTRACT);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_EXTRACT)){
			final Item[] options = new Item[EgoCatalog.ALL.length];
			String[] names = new String[options.length];
			for (int i = 0; i < options.length; i++){
				options[i] = Reflection.newInstance(EgoCatalog.ALL[i]);
				names[i] = Messages.titleCase(options[i].name());
			}

			GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()), Messages.get(this, "choose"), names){
				@Override
				protected void onSelect(int index) {
					Item item = options[index].identify();
					if (!item.collect()){
						Dungeon.level.drop(item, hero.pos).sprite.drop();
					}
					GLog.p(Messages.get(EgoExtractor.class, "extracted", item.name()));
				}

				@Override
				protected boolean hasIcon(int index) {
					return true;
				}

				@Override
				protected Image getIcon(int index) {
					return new ItemSprite(options[index]);
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
