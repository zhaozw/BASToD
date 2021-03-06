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
import java.util.Map;

import name.herve.bastod.engine.Engine;
import name.herve.bastod.engine.Player;
import name.herve.bastod.engine.PlayerAction;
import name.herve.bastod.engine.PlayerAction.Action;
import name.herve.bastod.engine.players.HumanPlayer;
import name.herve.bastod.guifwk.AbstractDisplayManager;
import name.herve.bastod.guifwk.GUIEvent;
import name.herve.bastod.guifwk.GUIResources;
import name.herve.bastod.guifwk.buttons.GUIButtonListener;
import name.herve.bastod.guifwk.layout.ComponentsLineLayout;
import name.herve.bastod.tools.math.Vector;

import com.badlogic.gdx.math.Vector3;

/**
 * @author Nicolas HERVE - n.herve@laposte.net
 */
public class PlayerManager extends AbstractDisplayManager implements GUIButtonListener {
	public enum State {
		BUY_TOWER, BUY_WALL, NOTHING
	}

	public final static String NAME_SEPARATOR = "!";
	public final static String NAME_START_STOP_SPAWN = "spawn";

	private String actingPlayer;
	private int cx;
	private int cy;
	private Engine engine;
	private float halfSQS;
	private Map<Player, PlayerInterface> interfaces;
	private boolean snapToGrid;
	private State state;

	public PlayerManager() {
		super();
		setSnapToGrid(true);
	}

	public void addHumanPlayer(HumanPlayer p, ComponentsLineLayout ptl, ComponentsLineLayout pml, boolean doSingleScoreBar) {
		PlayerInterface pi = new PlayerInterface(this, p, engine, getScreenHeight(), ptl, pml, doSingleScoreBar);
		p.setActionsProvider(pi);
		interfaces.put(p, pi);
	}

	@Override
	public void buttonActivated(GUIEvent event) {
		String[] params = event.getSource().getName().split(NAME_SEPARATOR);

		if (params[0].equals(NAME_START_STOP_SPAWN)) {
			Player player = engine.getPlayer(params[1]);
			PlayerAction pa = new PlayerAction(player, player.isSpawnEnabled() ? Action.STOP_SPAWN : Action.START_SPAWN);
			interfaces.get(pa.getPlayer()).addAction(pa);
		} else if (params[0].equals(Engine.IMP_BUY_TOWER)) {
			actingPlayer = params[1];
			state = State.BUY_TOWER;
		} else if (params[0].equals(Engine.IMP_BUY_WALL)) {
			actingPlayer = params[1];
			state = State.BUY_WALL;
		} else if (params[0].startsWith(Engine._IMPROVE)) {
			Player player = engine.getPlayer(params[1]);
			PlayerAction pa = new PlayerAction(player, Action.IMPROVE);
			pa.setParam(params[0]);
			interfaces.get(pa.getPlayer()).addAction(pa);
		}
	}

	public State getState() {
		return state;
	}

	public boolean keyDown(int k) {
		return false;
	}

	private Vector positionOnGrid(int x, int y) {
		// System.out.println("positionOnGrid("+x+", "+y+")");
		Vector3 touchPos = new Vector3();
		touchPos.set(x, y, 0);
		cameraUnproject(touchPos);
		Vector vm = new Vector(touchPos.x - Engine._SP_SIDE, touchPos.y - Engine._SP_BOTTOM);

		// if (vm.getX() < 0 || vm.getY() < 0) {
		// return null;
		// }

		vm = engine.fromBoardToGrid(vm);

		// System.out.println("  -> " + vm);

		return vm;
	}

	public void render() {
		for (PlayerInterface pi : interfaces.values()) {
			pi.render();
		}

		if (state == State.BUY_TOWER) {
			renderBuyTower(cx, cy, positionOnGrid(cx, cy), actingPlayer);
		}

		if (state == State.BUY_WALL) {
			renderBuyWall(cx, cy, positionOnGrid(cx, cy), actingPlayer);
		}
	}

	private void renderBuy(int x, int y, Vector pos, String sprite, String player) {
		// System.out.println("renderBuy " + pos);
		batchBegin();

		if (pos != null && engine.isOpenedBuildPosition(engine.getPlayer(player), pos)) {
			if (snapToGrid) {
				Vector v = engine.fromGridToBoard(pos);
				draw(GUIResources.getInstance().getSprite(sprite, player), Engine._SP_SIDE + v.getXInt() - halfSQS, Engine._SP_BOTTOM + v.getYInt() - halfSQS);
			} else {
				draw(GUIResources.getInstance().getSprite(sprite, player), x - halfSQS, getScreenHeight() - y - halfSQS);
			}
		} else {
			draw(GUIResources.getInstance().getSprite("noway"), x - halfSQS, getScreenHeight() - y - halfSQS);
		}

		batchEnd();
	}

	public void renderBuyTower(int x, int y, Vector pos, String p) {
		renderBuy(x, y, pos, "tower", p);
	}

	public void renderBuyWall(int x, int y, Vector pos, String p) {
		renderBuy(x, y, pos, "wall", p);
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	public void setSnapToGrid(boolean snapToGrid) {
		this.snapToGrid = snapToGrid;
	}

	@Override
	public void start() {
		super.start();

		state = State.NOTHING;
		halfSQS = engine.getGridSquareSize() / 2f;

		interfaces = new HashMap<Player, PlayerInterface>();
	}

	@Override
	public void stop() {
		super.stop();

		interfaces = null;
	}

	public boolean touchDown(int x, int y, int pointer, int button) {
		cx = x;
		cy = y;

		for (PlayerInterface pi : interfaces.values()) {
			if (pi.touchDown(x, getScreenHeight() - y, pointer, button)) {
				return true;
			}
		}

		if (state == State.BUY_TOWER) {
			state = State.NOTHING;
			if (button == 0) {
				Vector pog = positionOnGrid(cx, cy);
				if (pog != null) {
					PlayerAction pa = new PlayerAction(engine.getPlayer(actingPlayer), Action.BUY_TOWER);
					pa.setPositionOnGrid(pog);
					interfaces.get(pa.getPlayer()).addAction(pa);
				}
			}
		}

		if (state == State.BUY_WALL) {
			state = State.NOTHING;
			if (button == 0) {
				Vector pog = positionOnGrid(cx, cy);
				if (pog != null) {
					PlayerAction pa = new PlayerAction(engine.getPlayer(actingPlayer), Action.BUY_WALL);
					pa.setPositionOnGrid(pog);
					interfaces.get(pa.getPlayer()).addAction(pa);
				}
			}
		}

		return false;
	}

	public boolean touchMoved(int x, int y) {
		cx = x;
		cy = y;

		if ((state == State.BUY_TOWER) || (state == State.BUY_WALL)) {
			return true;
		}

		return false;
	}

	public void updateDisplayComponents() {
		for (PlayerInterface pi : interfaces.values()) {
			pi.updateDisplayComponents();
		}
	}
}
