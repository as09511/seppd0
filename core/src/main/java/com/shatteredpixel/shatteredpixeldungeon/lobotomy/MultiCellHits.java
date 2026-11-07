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

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;

//for a monster whose body fills several cells (the Food Chain, the Blue Star...): a blast or a cloud that covers
//several of its cells at once only harms it once, while separate hits on the same cell (several strikes of one
//attack, like Justitia's) all go through
public class MultiCellHits {

	private Object source = null;
	private float time = -1;
	private int cells = 0;

	//whether a hit on this cell of the body (0, 1, 2...) should go through
	public boolean allow( Object src, int cell ){
		if (src == null) return true;
		int bit = 1 << cell;
		if (src != source || Actor.now() != time || (cells & bit) != 0){
			//a new hit: from another source, at another time, or on a cell this one already struck
			source = src;
			time = Actor.now();
			cells = bit;
			return true;
		}
		//the same blast reaching another of its cells
		cells |= bit;
		return false;
	}
}
