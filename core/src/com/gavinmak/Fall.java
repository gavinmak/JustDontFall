package com.gavinmak;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Fall extends com.badlogic.gdx.Game {
    public SpriteBatch batch;
    public BitmapFont font;

    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        dispose();
        this.setScreen(new MainMenuScreen(this));
    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}