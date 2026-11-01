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


package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Analyzed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.InfoPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.Prediction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod.YesodTrackers;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Yesod's starting item: spends the information he gathers on his abilities
public class YesodScarf extends Item {

	public static final String AC_ANALYZE = "ANALYZE";
	public static final String AC_DETECT  = "DETECT";
	public static final String AC_PREDICT = "PREDICT";
	public static final String AC_REVIEW  = "REVIEW";

	public static final int ANALYZE_COST = 2;
	public static final int DETECT_COST  = 4;
	public static final int PREDICT_COST = 3;

	public static final int DETECT_RANGE = 8;

	{
		image = ItemSpriteSheet.YESOD_SCARF;

		defaultAction = AC_ANALYZE;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (hero.buff(MagicImmune.class) == null) {
			actions.add(AC_ANALYZE);
			actions.add(AC_DETECT);
			actions.add(AC_PREDICT);
		}
		if (reviewCharges(hero) > 0){
			actions.add(AC_REVIEW);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		switch (action){
			case AC_ANALYZE: return Messages.get(this, "ac_analyze", ANALYZE_COST);
			case AC_DETECT:  return Messages.get(this, "ac_detect", DETECT_COST);
			case AC_PREDICT: return Messages.get(this, "ac_predict", PREDICT_COST);
			case AC_REVIEW:  return Messages.get(this, "ac_review", reviewCharges(hero));
		}
		return super.actionName(action, hero);
	}

	private static boolean canPay( int cost ){
		if (InfoPower.current() < cost){
			GLog.w( Messages.get(YesodScarf.class, "no_info", cost) );
			return false;
		}
		return true;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (action.equals(AC_REVIEW)){
			openDocumentReview(hero);
			return;
		}

		if (hero.buff(MagicImmune.class) != null) return;

		switch (action){
			case AC_ANALYZE:
				if (canPay(ANALYZE_COST)) GameScene.selectCell(analyzer);
				break;
			case AC_DETECT:
				if (canPay(DETECT_COST)){
					InfoPower.get().spend(DETECT_COST);
					detect(hero);
					hero.sprite.operate(hero.pos);
					hero.spendAndNext(1f);
				}
				break;
			case AC_PREDICT:
				if (canPay(PREDICT_COST)){
					InfoPower.get().spend(PREDICT_COST);
					Buff.affect(hero, Prediction.Evade.class);
					Buff.affect(hero, Prediction.Strike.class);
					hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
					hero.sprite.operate(hero.pos);
					hero.spendAndNext(1f);
				}
				break;
		}
	}

	//reveals the map, traps and secrets within range. Secrets found this way give no info
	private static void detect( Hero hero ){
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;
		boolean noticed = false;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!discoverable[i] || Dungeon.level.distance(hero.pos, i) > DETECT_RANGE) continue;
			int terr = map[i];
			mapped[i] = true;
			if ((Terrain.flags[terr] & Terrain.SECRET) != 0){
				Dungeon.level.discover(i);
				if (Dungeon.level.heroFOV[i]){
					GameScene.discoverTile(i, terr);
					ScrollOfMagicMapping.discover(i);
					noticed = true;
				}
			}
		}
		GameScene.updateFog();
		Dungeon.observe();
		if (noticed) Sample.INSTANCE.play(Assets.Sounds.SECRET);
		SpellSprite.show(hero, SpellSprite.MAP);
		Sample.INSTANCE.play(Assets.Sounds.SCAN);
	}

	private static final CellSelector.Listener analyzer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero || !Dungeon.level.heroFOV[cell]){
				GLog.w(Messages.get(YesodScarf.class, "no_target"));
				return;
			}
			InfoPower p = InfoPower.get();
			if (p == null || !p.spend(ANALYZE_COST)) return;
			Analyzed.apply(ch, Analyzed.DURATION);
			if (ch.sprite != null){
				ch.sprite.emitter().burst(Speck.factory(Speck.DISCOVER), 8);
			}
			Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
			hero.sprite.zap(cell);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(YesodScarf.class, "analyze_prompt");
		}
	};

	//document review talent: free identifications, one per talent point
	public static int reviewCharges( Hero hero ){
		YesodTrackers.DocumentReviewCharges c = hero.buff(YesodTrackers.DocumentReviewCharges.class);
		return c == null ? 0 : (int)c.count();
	}

	public static void openDocumentReview( Hero hero ){
		if (reviewCharges(hero) <= 0) return;
		boolean anyUnknown = false;
		for (Item i : hero.belongings){
			if (!i.isIdentified()){
				anyUnknown = true;
				break;
			}
		}
		if (!anyUnknown){
			GLog.i(Messages.get(YesodScarf.class, "review_nothing"));
			return;
		}
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(YesodScarf.class, "review_prompt");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return !item.isIdentified();
			}

			@Override
			public void onSelect(Item item) {
				if (item == null || item.isIdentified()) return;
				YesodTrackers.DocumentReviewCharges c = Dungeon.hero.buff(YesodTrackers.DocumentReviewCharges.class);
				if (c == null || c.count() <= 0) return;
				c.countDown(1);
				if (c.count() <= 0) c.detach();
				item.identify();
				GLog.p(Messages.get(YesodScarf.class, "review_done", item.name()));
				Dungeon.hero.sprite.emitter().burst(Speck.factory(Speck.DISCOVER), 6);
				Sample.INSTANCE.play(Assets.Sounds.READ);
				updateQuickslot();
			}
		});
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String status() {
		if (Dungeon.hero == null) return null;
		return Messages.format("%d/%d", InfoPower.current(), InfoPower.max(Dungeon.hero));
	}

	@Override
	public String desc() {
		String desc = super.desc();
		desc += "\n\n" + Messages.get(this, "effect", ANALYZE_COST, (int)Analyzed.DURATION, DETECT_COST, DETECT_RANGE, PREDICT_COST);
		return desc;
	}

	@Override
	public int value() {
		return 0;
	}
}
