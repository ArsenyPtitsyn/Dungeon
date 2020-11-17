package ru.geekbrains.dungeon.units;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import ru.geekbrains.dungeon.GameController;
import ru.geekbrains.dungeon.GameMap;

public class Hero extends Unit {
    float movementTime;
    float movementMaxTime;
    int targetX, targetY;

    public Hero(TextureAtlas atlas, GameController gc) {
        super(gc, 1, 1, 10);
        this.texture = atlas.findRegion("knight");
        this.textureHp = atlas.findRegion("hp");
        this.movementMaxTime = 0.2f;
        this.targetX = cellX;
        this.targetY = cellY;
    }

    public void update(float dt) {
        checkMovement(dt);
    }

    public boolean isStayStill() {
        return cellY == targetY && cellX == targetX;
    }

    public void checkMovement(float dt) {
        if (Gdx.input.justTouched() && isStayStill()) {
            if (Math.abs(gc.getCursorX() - cellX) + Math.abs(gc.getCursorY() - cellY) == 1 &&
                    gc.getGameMap().isCellPossible(gc.getCursorX(), gc.getCursorY())) {
                targetX = gc.getCursorX();
                targetY = gc.getCursorY();
                moveCounter--;
                if (moveCounter < 0)
                    moveCounter = maxMoveCounter;
                System.out.println("Count of moves: " + moveCounter);
            }
        }

        Monster m = gc.getMonsterController().getMonsterInCell(targetX, targetY);
        if (m != null) {
            targetX = cellX;
            targetY = cellY;
            m.takeDamage(1);
            if (Math.random() * 4 < 1)
                this.takeDamage(1);
            if (m.hp == 0) {
                experience++;
            }
        }

        if (!gc.getGameMap().isCellPossible(targetX, targetY)) {
            targetX = cellX;
            targetY = cellY;
        }

        if (!isStayStill()) {
            movementTime += dt;
            if (movementTime > movementMaxTime) {
                movementTime = 0;
                cellX = targetX;
                cellY = targetY;
            }
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        float px = cellX * GameMap.CELL_SIZE;
        float py = cellY * GameMap.CELL_SIZE;
        if (!isStayStill()) {
            px = cellX * GameMap.CELL_SIZE + (targetX - cellX) * (movementTime / movementMaxTime) * GameMap.CELL_SIZE;
            py = cellY * GameMap.CELL_SIZE + (targetY - cellY) * (movementTime / movementMaxTime) * GameMap.CELL_SIZE;
        }
        batch.draw(texture, px, py);
        batch.setColor(0.0f, 0.0f, 0.0f, 1.0f);
        batch.draw(textureHp, px + 1, py + 51, 58, 10);
        batch.setColor(0.7f, 0.0f, 0.0f, 1.0f);
        batch.draw(textureHp, px + 2, py + 52, 56, 8);
        batch.setColor(0.0f, 1.0f, 0.0f, 1.0f);
        batch.draw(textureHp, px + 2, py + 52, (float) hp / hpMax * 56, 8);
        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        batch.draw(textureHp, px, py - 8, (float) moveCounter / maxMoveCounter * 60, 8);
        batch.setColor(1.0f, 1.0f, 0.0f, 1.0f);
        batch.draw(textureHp, px, py + 60, (float) experience / 10 * 60, 8); // полоска опыта...для 1 уровня героя
        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
