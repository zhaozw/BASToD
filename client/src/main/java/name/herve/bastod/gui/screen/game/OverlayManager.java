/*
 * Copyright 2012, 2013 Nicolas HERVE
 * 
 * This file is part of BASToD.
 * 
 * BASToD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BASToD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with BASToD. If not, see <http://www.gnu.org/licenses/>.
 */
package name.herve.bastod.gui.screen.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.herve.bastod.engine.Engine;
import name.herve.bastod.engine.EngineEvent;
import name.herve.bastod.engine.EngineListener;
import name.herve.bastod.engine.Player;
import name.herve.bastod.engine.Unit;
import name.herve.bastod.engine.buildings.Tower;
import name.herve.bastod.gui.components.UnitInfoBox;
import name.herve.bastod.guifwk.AbstractDisplayManager;
import name.herve.bastod.guifwk.GUIResources;
import name.herve.bastod.tools.math.Dimension;
import name.herve.bastod.tools.math.Vector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Texture;

/**
 * @author Nicolas HERVE - n.herve@laposte.net
 */
public class OverlayManager extends AbstractDisplayManager implements EngineListener {
	private Texture buildPositions;
	private Engine engine;
	private Texture grid;
//	private float halfSQS;
	
	private Unit lastSelected;
	private UnitInfoBox ibx;

	private Map<String, Texture> towerRanges;

	private void clearBuildPositionsCache() {
		if (buildPositions != null) {
			buildPositions.dispose();
			buildPositions = null;
		}
	}
	
	public void renderDebugLoS(Vector pos) {
		Blending bck = Pixmap.getBlending();
		Pixmap.setBlending(Blending.None);
		
		Dimension dimG = engine.getGridDimension();
		Dimension dimB = engine.getBoardDimension();
		Pixmap p = new Pixmap(dimB.getW() + 1, dimB.getH() + 1, Pixmap.Format.RGBA8888);
		
		Color ok = Color.GREEN;
		ok.a = 0.2f;
		Color nok = Color.ORANGE;
		nok.a = 0.2f;
		
		for (int x= 0; x < dimG.getW(); x++) {
			for (int y= 0; y < dimG.getH(); y++) {
				Vector v = new Vector(x, y);
				if (engine.lineOfSight(pos, v)) {
					drawTintedSquare(p, ok, engine.fromGridToBoard(v));
				} else {
					drawTintedSquare(p, nok, engine.fromGridToBoard(v));
				}
			}
		}
		
		Texture debug = new Texture(p);
		p.dispose();

		Pixmap.setBlending(bck);
		
		batchBegin();
		draw(debug, Engine._SP_SIDE, Engine._SP_BOTTOM);
		batchEnd();
		
		debug.dispose();
	}
	
	private void drawTintedSquare(Pixmap p, Color c, Vector v) {
		Dimension dimB = engine.getBoardDimension();
		int sqs = engine.getGridSquareSize();
		int step = (sqs - 2) / 7;
		int h = dimB.getH();
		
		int x = v.getXInt();
		int y = v.getYInt();
		
		p.setColor(c);
		
		for (int dx = step; dx < sqs; dx += step) {
			p.drawLine(x + dx, h - y, x + dx, h - y - sqs + 1);
		}
		
		for (int dy = step; dy < sqs; dy += step) {
			p.drawLine(x + 1, h - y - dy, x + sqs - 1, h - y - dy);
		}
	}

	@Override
	public void engineEvent(EngineEvent event) {
		switch (event.getType()) {
		case BOARD_MODIFIED:
			clearBuildPositionsCache();
			break;
		}
	}

	public void renderBackground() {
		batchBegin();
		draw(GUIResources.getInstance().getSprite("background"), Engine._SP_SIDE, Engine._SP_BOTTOM);
		batchEnd();
	}

	public void renderBuildPositions() {
		if (buildPositions == null) {
			Dimension dimB = engine.getBoardDimension();

			Blending bck = Pixmap.getBlending();
			Pixmap.setBlending(Blending.None);

			Pixmap p = new Pixmap(dimB.getW() + 1, dimB.getH() + 1, Pixmap.Format.RGBA8888);

			for (Player player : engine.getPlayers()) {
				Color c = GUIResources.getInstance().getColor(player.getColor()).cpy();
				c.a = 0.2f;

				List<Vector> pos = engine.getBuildPositions(player);
				if (pos != null) {
					for (Vector v : pos) {
						if ((Engine.PRECOMPUTE_OPEN_BUILD_POSITIONS && engine.isOpenedBuildPosition(v)) || (engine.isOpenedOnGrid(v))) {
							Vector vb = engine.fromGridToBoard(v);
							drawTintedSquare(p, c, vb);
						}
					}
				}
			}

			buildPositions = new Texture(p);
			p.dispose();

			Pixmap.setBlending(bck);
		}

		batchBegin();
		draw(buildPositions, Engine._SP_SIDE, Engine._SP_BOTTOM);
		batchEnd();
	}



	public void renderGrid() {
		if (grid == null) {
			Dimension dimG = engine.getGridDimension();
			Dimension dimB = engine.getBoardDimension();
			int sqs = engine.getGridSquareSize();

			Blending bck = Pixmap.getBlending();
			Pixmap.setBlending(Blending.None);

			Pixmap p = new Pixmap(dimB.getW() + 1, dimB.getH() + 1, Pixmap.Format.RGBA8888);
			Color c = Color.WHITE.cpy();
			c.a = 0.2f;
			p.setColor(c);

			for (int x = 0; x <= dimG.getW(); x++) {
				int bx = x * sqs;
				p.drawLine(bx, 0, bx, dimB.getH() - 1);
			}
			for (int y = 0; y <= dimG.getH(); y++) {
				int by = y * sqs;
				p.drawLine(0, by, dimB.getW() - 1, by);
			}

			grid = new Texture(p);
			p.dispose();

			Pixmap.setBlending(bck);
		}

		batchBegin();
		draw(grid, Engine._SP_SIDE, Engine._SP_BOTTOM);
		batchEnd();
	}
	
	public void renderUnitInfoBox(Unit selected) {
		if (selected != null) {
			if (selected != lastSelected) {
				if (ibx != null) {
					ibx.stop();
				}
				lastSelected = selected;
				ibx = new UnitInfoBox(selected);
				if (ibx.getX() + ibx.getWidth() > getScreenWidth()) {
					ibx.moveTo(selected.getPositionOnBoard().getXInt() - ibx.getWidth(), ibx.getY());
				}
				if (ibx.getY() - ibx.getHeight() < 0) {
					ibx.moveTo(ibx.getX(), selected.getPositionOnBoard().getYInt() + ibx.getHeight());
				}
				ibx.start();
			}
			
			ibx.render();
		}
	}
	
	public void renderUnitOverlay(Unit selected) {
		if (selected != null) {
			if (selected instanceof Tower) {
				int shotRange = (int) (((Tower) selected).getRangeOnBoard());
				String k = selected.getPlayer().getColor() + "-" + shotRange;

				if (!towerRanges.containsKey(k)) {
					int gfxSize = shotRange * 2;

					Blending bck = Pixmap.getBlending();
					Pixmap.setBlending(Blending.None);

					Pixmap p = new Pixmap(gfxSize, gfxSize, Pixmap.Format.RGBA8888);
					Color c = GUIResources.getInstance().getColor(selected.getPlayer().getColor()).cpy();
					c.a = 0.2f;
					p.setColor(c);
					p.fillCircle(shotRange, shotRange, shotRange);
					Texture t = new Texture(p);
					p.dispose();
					Pixmap.setBlending(bck);
					towerRanges.put(k, t);
				}

				batchBegin();
				draw(towerRanges.get(k), Engine._SP_SIDE + selected.getPositionOnBoard().getX() - shotRange, Engine._SP_BOTTOM + selected.getPositionOnBoard().getY() - shotRange);
				batchEnd();
			}
		}
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
		engine.addListener(this);
	}

	@Override
	public void start() {
		super.start();

		towerRanges = new HashMap<String, Texture>();
		grid = null;
//		halfSQS = engine.getGridSquareSize() / 2f;
	}

	@Override
	public void stop() {
		super.stop();

		if (grid != null) {
			grid.dispose();
			grid = null;
		}

		clearBuildPositionsCache();

		for (Texture t : towerRanges.values()) {
			t.dispose();
		}

		towerRanges = null;
	}

}
