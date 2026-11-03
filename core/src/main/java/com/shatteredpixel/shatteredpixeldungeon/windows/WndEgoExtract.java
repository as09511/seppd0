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


package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.EgoCatalog;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.Warehouse;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Reflection;

//Angela's E.G.O extraction: every E.G.O, grouped by risk level with its price in enkephalin boxes.
//Picking one asks for confirmation before anything is spent.
public class WndEgoExtract extends Window {

	private static final int WIDTH = 130;

	public WndEgoExtract( Angela angela ){
		super();

		ScrollingListPane list = new ScrollingListPane();
		add( list );

		list.addTitle(Messages.get(this, "title", Warehouse.enkephalin()));

		for (RiskLevel risk : RiskLevel.values()){
			boolean titled = false;
			for (Class<? extends Item> cls : EgoCatalog.ALL){
				Item item = Reflection.newInstance(cls);
				if (item == null || Warehouse.risk(item) != risk) continue;
				if (!titled){
					list.addTitle(Messages.get(this, "risk", risk.title(), Warehouse.extractCost(risk)));
					titled = true;
				}
				item.identify();
				ScrollingListPane.ListItem row = new ScrollingListPane.ListItem(
						new ItemSprite(item), null, Messages.titleCase(item.name())){
					@Override
					public boolean onClick( float x, float y ) {
						if (!inside(x, y)) return false;
						confirm(angela, cls, item);
						return true;
					}
				};
				list.addItem(row);
			}
		}

		int height = Math.min((int)(PixelScene.uiCamera.height * 0.8f), (int)list.content().height());
		resize(WIDTH, height);
		list.setRect(0, 0, width, height);
	}

	private void confirm( Angela angela, Class<? extends Item> cls, Item item ){
		int cost = Warehouse.extractCost(Warehouse.risk(item));
		boolean enough = Warehouse.enkephalin() >= cost;
		String text = Messages.get(this, "confirm", item.name(), cost, Warehouse.enkephalin());
		if (!enough) text += "\n\n" + Messages.get(Angela.class, "not_enough");
		ShatteredPixelDungeon.scene().addToFront(new WndOptions(
				new ItemSprite(item),
				Messages.titleCase(item.name()),
				text,
				Messages.get(this, "yes", cost),
				Messages.get(this, "no")){
			@Override
			protected boolean enabled( int index ) {
				return index != 0 || enough;
			}

			//the info button beside the extract option shows the full description and stats
			@Override
			protected boolean hasInfo( int index ) {
				return index == 0;
			}

			@Override
			protected void onInfo( int index ) {
				ShatteredPixelDungeon.scene().addToFront(new WndInfoItem(item));
			}

			@Override
			protected void onSelect( int index ) {
				if (index == 0){
					WndEgoExtract.this.hide();
					angela.extract(cls);
				}
			}
		});
	}
}
