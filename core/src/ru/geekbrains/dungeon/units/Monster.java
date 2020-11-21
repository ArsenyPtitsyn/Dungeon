package ru.geekbrains.dungeon.units;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import ru.geekbrains.dungeon.GameController;

public class Monster extends Unit {
    private float aiBrainsImplseTime;
    private Unit target;

    public Monster(TextureAtlas atlas, GameController gc) {
        super(gc, 5, 2, 10);
        this.texture = atlas.findRegion("monster");
        this.textureHp = atlas.findRegion("hp");
        this.hp = -1;
    }

    public void activate(int cellX, int cellY) {
        this.cellX = cellX;
        this.cellY = cellY;
        this.targetX = cellX;
        this.targetY = cellY;
        this.hpMax = 10;
        this.hp = hpMax;
        this.target = gc.getUnitController().getHero();
    }

    public void update(float dt) {
        super.update(dt);
        if (canIMakeAction()) {
            if (isStayStill()) {
                aiBrainsImplseTime += dt;
            }
            if (aiBrainsImplseTime > 0.4f) {
                aiBrainsImplseTime = 0.0f;
                if (canIAttackThisTarget(target)) {
                    attack(target);
                } else {
                    tryToMove();
                }
            }
        }
    }

    public void tryToMove() {
        final float aggrDst = 5.0f;
        int nextX = -1, nextY = -1;
        float bestDst = 10000;
        float dst = (float) Math.sqrt((cellX - target.getCellX()) * (cellX - target.getCellX())
                + (cellY - target.getCellY()) * (cellY - target.getCellY()));
        if (dst <= aggrDst) {
            for (int i = cellX - 1; i <= cellX + 1; i++) {
                for (int j = cellY - 1; j <= cellY + 1; j++) {
                    if (Math.abs(cellX - i) + Math.abs(cellY - j) == 1 && gc.getGameMap().isCellPassable(i, j)
                            && gc.getUnitController().isCellFree(i, j)) {
                        float newDst = (float) Math.sqrt((i - target.getCellX()) * (i - target.getCellX())
                                + (j - target.getCellY()) * (j - target.getCellY()));
                        if (newDst < bestDst) {
                            bestDst = newDst;
                            nextX = i;
                            nextY = j;
                        }
                    }
                }
            }
        } else {
            int random = MathUtils.random(3);
            if (random == 0 && gc.getGameMap().isCellPassable(cellX + 1, cellY)
                    && gc.getUnitController().isCellFree(cellX + 1, cellY)) {
                nextX = cellX + 1;
                nextY = cellY;
            } else if (random == 1 && gc.getGameMap().isCellPassable(cellX + 1, cellY)
                    && gc.getUnitController().isCellFree(cellX + 1, cellY)) {
                nextX = cellX;
                nextY = cellY + 1;
            } else if (random == 2 && gc.getGameMap().isCellPassable(cellX + 1, cellY)
                    && gc.getUnitController().isCellFree(cellX + 1, cellY)) {
                nextX = cellX -1;
                nextY = cellY;
            } else if (random == 3 && gc.getGameMap().isCellPassable(cellX + 1, cellY)
                    && gc.getUnitController().isCellFree(cellX + 1, cellY)) {
                nextX = cellX;
                nextY = cellY - 1;
            }
        }
        goTo(nextX, nextY);
    }
}
