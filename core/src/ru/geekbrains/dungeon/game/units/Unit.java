package ru.geekbrains.dungeon.game.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import lombok.Data;
import ru.geekbrains.dungeon.game.*;
import ru.geekbrains.dungeon.helpers.Assets;
import ru.geekbrains.dungeon.helpers.Poolable;

@Data
public abstract class Unit implements Poolable {
    public enum Direction {
        LEFT(0), RIGHT(1), UP(2), DOWN(3);

        int animationRowIndex;

        Direction(int animationRowIndex) {
            this.animationRowIndex = animationRowIndex;
        }

        public static Direction getMoveDirection(int fromX, int fromY, int toX, int toY) {
            int dx = toX - fromX;
            int dy = toY - fromY;
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx < 0) {
                    return LEFT;
                } else if (dx > 0) {
                    return RIGHT;
                }
            }
            if (Math.abs(dx) < Math.abs(dy)) {
                if (dy < 0) {
                    return DOWN;
                } else if (dy > 0) {
                    return UP;
                }
            }
            return DOWN;
        }
    }

    GameController gc;
    TextureRegion[][] textures;
    TextureRegion textureHp;
    Stats stats;

    int cellX;
    int cellY;
    int gold;
    int satiety, maxSatiety;
    float movementTime;
    float movementMaxTime;
    int targetX, targetY;
    Weapon weapon;
    Armour armour;

    float innerTimer;
    StringBuilder stringHelper;
    Direction currentDirection;

    float timePerFrame;
    float walkingTime;

    int getCellCenterX() {
        return cellX * GameMap.CELL_SIZE + GameMap.CELL_SIZE / 2;
    }

    int getCellCenterY() {
        return cellY * GameMap.CELL_SIZE + GameMap.CELL_SIZE / 2;
    }

    int getCellTopY() {
        return (cellY + 1) * GameMap.CELL_SIZE;
    }

    public Unit(GameController gc, int cellX, int cellY, int hpMax, String textureName) {
        this.gc = gc;
        this.stats = new Stats(1, hpMax, 1, 5, 1, 5);
        this.cellX = cellX;
        this.cellY = cellY;
        this.targetX = cellX;
        this.targetY = cellY;
        this.movementMaxTime = 0.4f;
        this.timePerFrame = 0.1f;
        this.innerTimer = MathUtils.random(1000.0f);
        this.stringHelper = new StringBuilder();
        this.gold = MathUtils.random(1, 5);
        this.textures = Assets.getInstance().getAtlas().findRegion(textureName).split(60, 60);
        this.currentDirection = Direction.DOWN;
        this.armour = gc.getArmourController().getArmourByIndex(0);
        this.maxSatiety = 20;
        this.satiety = maxSatiety;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public void cure(int amount) {
        stats.restoreHp(amount);
    }

    public void addSatiety(int amount) {
        if (satiety < maxSatiety + amount)
            satiety += amount;
        else
            satiety = maxSatiety;
    }

    public void reduceSatiety(int amount) {
        if (satiety + amount > 0)
            satiety -= amount;
        else if (satiety > 0 && amount > satiety){
            satiety = 0;
            stats.hp -= amount-satiety;
        } else {
            satiety = 0;
            stats.hp -= amount;
        }
    }

    public void startTurn() {
        stats.restorePoints();
    }

    public void startRound() {
        cure(1);
    }

    @Override
    public boolean isActive() {
        return stats.hp > 0;
    }

    public boolean takeDamage(Unit source, int amount) {
        stats.hp -= amount;
        stringHelper.setLength(0);
        if (amount > 0) {
            stringHelper.append(-amount);
        } else {
            stringHelper.append("Blocked");
        }
        Color currentColor = Color.WHITE;
        if (gc.getUnitController().isItMyTurn(this)) {
            currentColor = Color.RED;
        }
        gc.getInfoController().setup(getCellCenterX(), getCellTopY(), stringHelper, currentColor);
        if (stats.hp <= 0) {
            gc.getUnitController().removeUnitAfterDeath(this);
            gc.getGameMap().generateDrop(cellX, cellY, 1);
        }
        return stats.hp <= 0;
    }

    public boolean canIMakeAction() {
        return gc.getUnitController().isItMyTurn(this) && getStats().doIHaveAnyPoints() && isStayStill();
    }

    public boolean isStayStill() {
        return cellY == targetY && cellX == targetX;
    }

    public void goTo(int argCellX, int argCellY) {
        if (!gc.isCellEmpty(argCellX, argCellY)) {
            return;
        }

        if (Math.abs(argCellX - cellX) + Math.abs(argCellY - cellY) == 1 &&
                stats.movePoints >= gc.getGameMap().getCostOfPassage(argCellX, argCellY)) {
            targetX = argCellX;
            targetY = argCellY;
            currentDirection = Direction.getMoveDirection(cellX, cellY, targetX, targetY);
        }
    }

    public boolean canIAttackThisTarget(Unit target, int cost) {
        return stats.attackPoints >= cost && (cellX - target.getCellX() == 0 && Math.abs(cellY - target.getCellY()) <= weapon.getRadius() ||
                cellY - target.getCellY() == 0 && Math.abs(cellX - target.getCellX()) <= weapon.getRadius());
    }

    public void attack(Unit target) {
        currentDirection = Direction.getMoveDirection(cellX, cellY, target.cellX, target.cellY);
        target.takeDamage(this, BattleCalc.attack(this, target));

        if (target.canIAttackThisTarget(this, 0)) {
            boolean shouldICounterAttack = BattleCalc.rollCounterAttack(this, target);
            if (shouldICounterAttack) {
                this.takeDamage(target, BattleCalc.checkCounterAttack(this, target));
            }
        }
        stats.attackPoints--;

        gc.getEffectController().setup(target.getCellCenterX(), target.getCellCenterY(), weapon.getFxIndex());
    }

    public void gatherBerry(int berryCellX, int berryCellY) {
        currentDirection = Direction.getMoveDirection(cellX, cellY, berryCellX, berryCellY);
        if (canIGatherBerry(berryCellX, berryCellY)) {
            gc.getGameMap().removeBerry(berryCellX, berryCellY);
            stats.movePoints -= gc.getGameMap().getCostOfPassage(cellX, cellY);
            if (satiety < maxSatiety)
                addSatiety(1);
        }
    }

    public boolean canIGatherBerry(int berryCellX, int berryCellY) {
        return stats.movePoints >= gc.getGameMap().getCostOfPassage(cellX, cellY) && satiety < maxSatiety &&
                ((cellX - berryCellX == 0 && Math.abs(cellY - berryCellY) == 1) ||
                (cellY - berryCellY == 0 && Math.abs(cellX - berryCellX) == 1)) &&
                gc.getGameMap().isCellWithBerries(berryCellX, berryCellY);
    }

    public void update(float dt) {
        innerTimer += dt;
        if (!isStayStill()) {
            movementTime += dt;
            walkingTime += dt;
            if (movementTime > movementMaxTime) {
                movementTime = 0;
                cellX = targetX;
                cellY = targetY;
                stats.movePoints -= gc.getGameMap().getCostOfPassage(targetX, targetY);
                gc.getGameMap().checkAndTakeDrop(this);
            }
        }
    }

    public void render(SpriteBatch batch, BitmapFont font18) {
        float hpAlpha = stats.hp == stats.maxHp ? 0.4f : 1.0f;

        float px = cellX * GameMap.CELL_SIZE;
        float py = cellY * GameMap.CELL_SIZE;

        if (!isStayStill()) {
            px = cellX * GameMap.CELL_SIZE + (targetX - cellX) * (movementTime / movementMaxTime) * GameMap.CELL_SIZE;
            py = cellY * GameMap.CELL_SIZE + (targetY - cellY) * (movementTime / movementMaxTime) * GameMap.CELL_SIZE;
        }

        int frameIndex = (int) (walkingTime / timePerFrame) % textures[0].length;
        batch.draw(textures[currentDirection.animationRowIndex][frameIndex], px, py);
        batch.setColor(0.0f, 0.0f, 0.0f, hpAlpha);

        float barX = px, barY = py + MathUtils.sin(innerTimer * 5.0f) * 2;
        batch.draw(textureHp, barX + 1, barY + 51, 58, 10);
        batch.setColor(0.7f, 0.0f, 0.0f, hpAlpha);
        batch.draw(textureHp, barX + 2, barY + 52, 56, 8);
        batch.setColor(0.0f, 1.0f, 0.0f, hpAlpha);
        batch.draw(textureHp, barX + 2, barY + 52, (float) stats.hp / stats.maxHp * 56, 8);
        batch.setColor(1.0f, 1.0f, 1.0f, hpAlpha);
        stringHelper.setLength(0);
        stringHelper.append(stats.hp);
        font18.setColor(1.0f, 1.0f, 1.0f, hpAlpha);
        font18.draw(batch, stringHelper, barX, barY + 64, 60, 1, false);

        font18.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (gc.getUnitController().isItMyTurn(this)) {
            stringHelper.setLength(0);
            stringHelper.append("MP: ").append(stats.movePoints).append(" AP: ").append(stats.attackPoints);
            font18.draw(batch, stringHelper, barX, barY + 80, 60, 1, false);
        }
        batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }


    public boolean amIBlocked() {
        return !(gc.isCellEmpty(cellX - 1, cellY) || gc.isCellEmpty(cellX + 1, cellY) || gc.isCellEmpty(cellX, cellY - 1) || gc.isCellEmpty(cellX, cellY + 1));
    }
}
