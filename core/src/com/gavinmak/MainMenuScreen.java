package com.gavinmak;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Created by gavin on 12/24/16.
 */

public class MainMenuScreen implements Screen{
    public final Fall fall;
    public OrthographicCamera camera;
    private float alpha = 0f, alphaText = 0f;

    private boolean fadeStarted = false;
    private BitmapFont textFont;
    private ShapeRenderer fade;
    private int screenWidth = Gdx.graphics.getWidth(), screenHeight = Gdx.graphics.getHeight();

    public MainMenuScreen(final Fall gam) {
        this.fall = gam;

        FreeTypeFontGenerator textGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter textParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParameter.size = screenWidth / 4;
        textParameter.color = Color.BLACK;
        textFont = textGenerator.generateFont(textParameter);
        textGenerator.dispose();

        fade = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, screenWidth, screenHeight);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        fall.batch.setProjectionMatrix(camera.combined);

        Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fade.begin(ShapeRenderer.ShapeType.Filled);
        fade.setColor(new Color(0, 0, 0, alpha));
        fade.rect(0, 0, screenWidth, screenHeight);
        fade.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        fall.batch.begin();
        textFont.draw(fall.batch, "just dont \n fall", screenWidth * 0.05f, screenHeight * 0.9f);
        if((delta * 30) % 2 == 0)
            textFont.draw(fall.batch, ">okay", screenWidth * 0.45f, screenHeight * 0.2f);
        else
            textFont.draw(fall.batch, "okay", screenWidth * 0.55f, screenHeight * 0.2f);
        fall.batch.end();

        if(Gdx.input.isTouched()) {
            fadeStarted = true;
        }

        if(fadeStarted)
            alpha += 0.025;

        if(alpha > 1) {
            fall.setScreen(new GameScreen(fall));
            dispose();
        }


    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void show() {

    }


    @Override
    public void dispose() {

    }
}
