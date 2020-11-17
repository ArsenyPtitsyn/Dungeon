package ru.geekbrains.dungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Hero {
    private float angle;
    private final ProjectileController projectileController;
    private final Vector2 position;
    private final float speedModule;
    private final Vector2 velocity;
    private final TextureRegion texture;
    private final float projectileSpeedModule;
    private int shootingMode;

    public Hero(TextureAtlas atlas, ProjectileController projectileController) {
        this.position = new Vector2(100, 100);
        this.velocity = new Vector2(0, 0);
        this.texture = atlas.findRegion("tank");
        this.projectileController = projectileController;
        this.shootingMode = 0;
        this.speedModule = 90;
        this.angle = 0;
        this.projectileSpeedModule = 200;
    }

    private void changeShootingMode() {
        if (shootingMode == 0)
            shootingMode = 1;
        else if (shootingMode == 1)
            shootingMode = 0;
    }

    public void update(float dt) {
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            angle = 90;
            velocity.set(speedModule * MathUtils.cosDeg(angle), speedModule * MathUtils.sinDeg(angle));
            position.mulAdd(velocity, dt);
            if (position.y > 600)
                position.set(position.x, 600);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            angle = -90;
            velocity.set(speedModule * MathUtils.cosDeg(angle), speedModule * MathUtils.sinDeg(angle));
            position.mulAdd(velocity, dt);
            if (position.y < 20)
                position.set(position.x,20);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            angle = 180;
            velocity.set(speedModule * MathUtils.cosDeg(angle), speedModule * MathUtils.sinDeg(angle));
            position.mulAdd(velocity, dt);
            if (position.x < 20)
                position.set(20, position.y);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            angle = 0;
            velocity.set(speedModule * MathUtils.cosDeg(angle), speedModule * MathUtils.sinDeg(angle));
            position.mulAdd(velocity, dt);
            if (position.x > 1000)
                position.set(1000, position.y);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            changeShootingMode();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (shootingMode == 0)
                projectileController.activate(position.x, position.y,
                        projectileSpeedModule * MathUtils.cosDeg(angle),
                        projectileSpeedModule * MathUtils.sinDeg(angle));
            else if (shootingMode == 1) {
                projectileController.activate(position.x, position.y,
                        projectileSpeedModule * MathUtils.cosDeg(angle + 5),
                        projectileSpeedModule * MathUtils.sinDeg(angle + 5));
                projectileController.activate(position.x, position.y,
                        projectileSpeedModule * MathUtils.cosDeg(angle - 5),
                        projectileSpeedModule * MathUtils.sinDeg(angle - 5));
            }
        }
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x - 20, position.y - 20);
    }
}
