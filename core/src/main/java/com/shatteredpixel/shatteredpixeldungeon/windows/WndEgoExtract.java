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
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//E.G.O extraction, for Angela (every E.G.O but combined ones, priced in enkephalin boxes) or the tester's
//extractor (every E.G.O, free), grouped by risk level.
//A risk level is chosen first, then its E.G.O are listed PAGE_SIZE to a page.
//Picking one asks for confirmation before anything is spent.
public class WndEgoExtract extends Window {

	private static final int WIDTH = 130;
	private static final int PAGE_SIZE = 8;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP = 2;

	private ScrollingListPane list;

	//what the list is for: Angela's paid extraction, or the tester's free extractor
	public interface Picker {
		boolean includes( Item item );
		String title();
		String riskTitle( RiskLevel risk );
		void pick( WndEgoExtract wnd, Class<? extends Item> cls, Item item );
	}

	//Angela: every E.G.O except combined ones, priced in enkephalin boxes with a confirmation
	private static class AngelaPicker implements Picker {
		private final Angela angela;
		AngelaPicker( Angela angela ){ this.angela = angela; }

		@Override
		public boolean includes( Item item ) {
			//combined E.G.O can only be made at an alchemy pot
			return !(item instanceof CombinedEgo);
		}

		@Override
		public String title() {
			return Messages.get(WndEgoExtract.class, "title", Warehouse.enkephalin());
		}

		@Override
		public String riskTitle( RiskLevel risk ) {
			return Messages.get(WndEgoExtract.class, "risk", risk.title(), Warehouse.extractCost(risk));
		}

		@Override
		public void pick( WndEgoExtract wnd, Class<? extends Item> cls, Item item ) {
			wnd.confirm(angela, cls, item);
		}
	}

	private final Picker picker;

	//every E.G.O of a risk level the picker offers, in catalog order
	private static ArrayList<Class<? extends Item>> egoOf( Picker picker, RiskLevel risk ){
		ArrayList<Class<? extends Item>> all = new ArrayList<>();
		for (Class<? extends Item> cls : EgoCatalog.ALL){
			Item item = Reflection.newInstance(cls);
			if (item == null || !picker.includes(item) || Warehouse.risk(item) != risk) continue;
			all.add(cls);
		}
		return all;
	}

	//Angela's extraction starts by choosing a risk level
	public static void open( Angela angela ){
		open(new AngelaPicker(angela));
	}

	public static void open( Picker picker ){
		final ArrayList<RiskLevel> risks = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();
		for (RiskLevel risk : RiskLevel.values()){
			int count = egoOf(picker, risk).size();
			if (count > 0){
				risks.add(risk);
				names.add(picker.riskTitle(risk) + " (" + count + ")");
			}
		}
		GameScene.show(new WndOptions(picker.title(), Messages.get(WndEgoExtract.class, "choose_risk"),
				names.toArray(new String[0])){
			@Override
			protected void onSelect( int index ) {
				GameScene.show(new WndEgoExtract(picker, risks.get(index), 0));
			}
		});
	}

	//one risk level, PAGE_SIZE E.G.O per page
	public WndEgoExtract( Picker picker, RiskLevel risk, int index ){
		super();
		this.picker = picker;

		ArrayList<Class<? extends Item>> all = egoOf(picker, risk);
		int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		index = Math.max(0, Math.min(index, pages-1));
		final int current = index;

		list = new ScrollingListPane();
		add( list );

		list.addTitle(picker.title());
		String riskTitle = picker.riskTitle(risk);
		if (pages > 1) riskTitle += " (" + (current+1) + "/" + pages + ")";
		list.addTitle(riskTitle);

		for (Class<? extends Item> cls : all.subList(current*PAGE_SIZE, Math.min(all.size(), (current+1)*PAGE_SIZE))){
			Item item = Reflection.newInstance(cls);
			//a display copy: shows its stats without marking it as seen in the catalog
			item.identify(false);
			ScrollingListPane.ListItem row = new ScrollingListPane.ListItem(
					new ItemSprite(item), null, Messages.titleCase(item.name())){
				@Override
				public boolean onClick( float x, float y ) {
					if (!inside(x, y)) return false;
					picker.pick(WndEgoExtract.this, cls, item);
					return true;
				}
			};
			list.addItem(row);
		}

		RedButton prev = new RedButton(Messages.get(this, "prev")){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndEgoExtract(picker, risk, current-1));
			}
		};
		prev.enable(current > 0);
		add(prev);

		//back to choosing a risk level
		RedButton back = new RedButton(Messages.get(this, "back")){
			@Override
			protected void onClick() {
				hide();
				open(picker);
			}
		};
		add(back);

		RedButton next = new RedButton(Messages.get(this, "next")){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndEgoExtract(picker, risk, current+1));
			}
		};
		next.enable(current < pages-1);
		add(next);

		int listHeight = Math.min((int)(PixelScene.uiCamera.height * 0.9f) - BTN_HEIGHT - GAP, (int)list.content().height());
		resize(WIDTH, listHeight + GAP + BTN_HEIGHT);
		list.setRect(0, 0, width, listHeight);
		float btnW = (WIDTH - 2*GAP) / 3f;
		prev.setRect(0, listHeight + GAP, btnW, BTN_HEIGHT);
		back.setRect(btnW + GAP, listHeight + GAP, btnW, BTN_HEIGHT);
		next.setRect(2*(btnW + GAP), listHeight + GAP, btnW, BTN_HEIGHT);
	}

	//the game moves windows aside while the inventory is open, the list has to follow
	@Override
	public void offset( int xOffset, int yOffset ) {
		super.offset(xOffset, yOffset);
		if (list != null) list.setRect(list.left(), list.top(), list.width(), list.height());
	}

	void confirm( Angela angela, Class<? extends Item> cls, Item item ){
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
