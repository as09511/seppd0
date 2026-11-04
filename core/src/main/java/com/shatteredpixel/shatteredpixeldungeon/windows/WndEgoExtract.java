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
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.CombinedEgo;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.EgoCatalog;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.Warehouse;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//Angela's E.G.O extraction: every E.G.O, grouped by risk level with its price in enkephalin boxes.
//Each page holds one risk level, at most PAGE_SIZE E.G.O; a level with more is split over several pages.
//Picking one asks for confirmation before anything is spent.
public class WndEgoExtract extends Window {

	private static final int WIDTH = 130;
	private static final int PAGE_SIZE = 8;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 2;

	private ScrollingListPane list;

	//one page of the list: a risk level and up to PAGE_SIZE E.G.O of it
	private static class Page {
		RiskLevel risk;
		ArrayList<Class<? extends Item>> items = new ArrayList<>();
		int part, parts;
	}

	private static ArrayList<Page> pages(){
		ArrayList<Page> pages = new ArrayList<>();
		for (RiskLevel risk : RiskLevel.values()){
			ArrayList<Class<? extends Item>> all = new ArrayList<>();
			for (Class<? extends Item> cls : EgoCatalog.ALL){
				Item item = Reflection.newInstance(cls);
				//combined E.G.O can only be made at an alchemy pot
				if (item == null || item instanceof CombinedEgo || Warehouse.risk(item) != risk) continue;
				all.add(cls);
			}
			int parts = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
			for (int i = 0; i < parts; i++){
				Page page = new Page();
				page.risk = risk;
				page.items.addAll(all.subList(i*PAGE_SIZE, Math.min(all.size(), (i+1)*PAGE_SIZE)));
				page.part = i+1;
				page.parts = parts;
				pages.add(page);
			}
		}
		return pages;
	}

	public WndEgoExtract( Angela angela ){
		this(angela, 0);
	}

	public WndEgoExtract( Angela angela, int index ){
		super();

		ArrayList<Page> pages = pages();
		index = Math.max(0, Math.min(index, pages.size()-1));
		final int current = index;
		Page page = pages.get(index);

		list = new ScrollingListPane();
		add( list );

		list.addTitle(Messages.get(this, "title", Warehouse.enkephalin()));
		String riskTitle = Messages.get(this, "risk", page.risk.title(), Warehouse.extractCost(page.risk));
		if (page.parts > 1) riskTitle += " (" + page.part + "/" + page.parts + ")";
		list.addTitle(riskTitle);

		for (Class<? extends Item> cls : page.items){
			Item item = Reflection.newInstance(cls);
			//a display copy: shows its stats without marking it as seen in the catalog
			item.identify(false);
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

		RedButton prev = new RedButton(Messages.get(this, "prev")){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndEgoExtract(angela, current-1));
			}
		};
		prev.enable(current > 0);
		add(prev);

		RenderedTextBlock pageNo = PixelScene.renderTextBlock((current+1) + " / " + pages.size(), 8);
		add(pageNo);

		RedButton next = new RedButton(Messages.get(this, "next")){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndEgoExtract(angela, current+1));
			}
		};
		next.enable(current < pages.size()-1);
		add(next);

		int listHeight = Math.min((int)(PixelScene.uiCamera.height * 0.9f) - BTN_HEIGHT - GAP, (int)list.content().height());
		resize(WIDTH, listHeight + GAP + BTN_HEIGHT);
		list.setRect(0, 0, width, listHeight);
		int btnW = (WIDTH - 30) / 2;
		prev.setRect(0, listHeight + GAP, btnW, BTN_HEIGHT);
		next.setRect(WIDTH - btnW, listHeight + GAP, btnW, BTN_HEIGHT);
		pageNo.setPos(btnW + (WIDTH - 2*btnW - pageNo.width())/2f, listHeight + GAP + (BTN_HEIGHT - pageNo.height())/2f);
		PixelScene.align(pageNo);
	}

	//the game moves windows aside while the inventory is open, the list has to follow
	@Override
	public void offset( int xOffset, int yOffset ) {
		super.offset(xOffset, yOffset);
		if (list != null) list.setRect(list.left(), list.top(), list.width(), list.height());
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
